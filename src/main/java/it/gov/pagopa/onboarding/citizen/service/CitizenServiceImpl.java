package it.gov.pagopa.onboarding.citizen.service;

import it.gov.pagopa.common.utils.Utils;
import it.gov.pagopa.onboarding.citizen.configuration.ExceptionMap;
import it.gov.pagopa.onboarding.citizen.connector.tpp.TppConnectorImpl;
import it.gov.pagopa.onboarding.citizen.constants.CitizenConstants.ExceptionMessage;
import it.gov.pagopa.onboarding.citizen.constants.CitizenConstants.ExceptionName;
import it.gov.pagopa.onboarding.citizen.dto.CitizenConsentDTO;
import it.gov.pagopa.onboarding.citizen.dto.TppIdList;
import it.gov.pagopa.onboarding.citizen.dto.mapper.CitizenConsentObjectToDTOMapper;
import it.gov.pagopa.onboarding.citizen.model.CitizenConsent;
import it.gov.pagopa.onboarding.citizen.model.ConsentDetails;
import it.gov.pagopa.onboarding.citizen.repository.CitizenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>Implementation of {@link CitizenService}.</p>
 */
@Service
@Slf4j
public class CitizenServiceImpl implements CitizenService {

    private final CitizenRepository citizenRepository;
    private final CitizenConsentObjectToDTOMapper mapperToDTO;
    private final ExceptionMap exceptionMap;
    private final TppConnectorImpl tppConnector;
    private final BloomFilterServiceImpl bloomFilterService;
    private static final String CONSENT_NOT_FOUND = "[EMD-CITIZEN][FIND-CITIZEN-CONSENTS-ENABLED] No consents found.";

    public CitizenServiceImpl(CitizenRepository citizenRepository,
                              CitizenConsentObjectToDTOMapper mapperToDTO,
                              ExceptionMap exceptionMap,
                              TppConnectorImpl tppConnector,
                              BloomFilterServiceImpl bloomFilterService) {
        this.citizenRepository = citizenRepository;
        this.mapperToDTO = mapperToDTO;
        this.exceptionMap = exceptionMap;
        this.tppConnector = tppConnector;
        this.bloomFilterService = bloomFilterService;
    }

    /**
     * <p>Creates or reuses a consent for the given fiscal code and TPP id.</p>
     *
     * <p>Flow:</p>
     * <ol>
     *   <li>Log input (hashed fiscal code, tppId).</li>
     *   <li>Validate TPP existence (remote call).</li>
     *   <li>Load existing citizen aggregate.</li>
     *   <li>If present and missing the consent, add enabled consent and persist.</li>
     *   <li>If absent, create aggregate, persist, add to Bloom filter.</li>
     *   <li>Reduce map to the requested TPP and return DTO.</li>
     * </ol>
     *
     * <p>Errors:</p>
     * <ul>
     *   <li>{@code TPP_NOT_FOUND} if remote TPP is missing.</li>
     *   <li>Repository errors propagate.</li>
     * </ul>
     *
     * @param fiscalCode plain fiscal code (hashed only in logs)
     * @param tppId TPP identifier
     * @return {@code Mono<CitizenConsentDTO>} DTO limited to the requested consent
     */
    @Override
    public Mono<CitizenConsentDTO> createCitizenConsent(String fiscalCode, String tppId){
        log.info("[EMD-CITIZEN][CREATE-CITIZEN-CONSENT] Received hashedFiscalCode: {} and tppId: {}",
                Utils.createSHA256(fiscalCode), tppId);

        return tppConnector.get(tppId)
                .onErrorMap(error -> exceptionMap.throwException(ExceptionName.TPP_NOT_FOUND, ExceptionMessage.TPP_NOT_FOUND))
                .flatMap(tppResponse -> citizenRepository.findByFiscalCode(fiscalCode)
                        .flatMap(citizenConsent -> {
                            if (!citizenConsent.getConsents().containsKey(tppId)) {
                                citizenConsent
                                        .getConsents().put(tppId, ConsentDetails.builder()
                                        .tppState(true)
                                        .tcDate(LocalDateTime.now())
                                        .build());
                                return citizenRepository.save(citizenConsent)
                                        .flatMap(savedConsent ->{
                                            Map<String, ConsentDetails> consents = new HashMap<>();
                                            consents.put(tppId, citizenConsent.getConsents().get(tppId));
                                            citizenConsent.setConsents(consents);
                                            return Mono.just(mapperToDTO.map(citizenConsent));
                                        });
                            }
                            Map<String, ConsentDetails> consents = new HashMap<>();
                            consents.put(tppId, citizenConsent.getConsents().get(tppId));
                            citizenConsent.setConsents(consents);
                            return Mono.just(mapperToDTO.map(citizenConsent));
                        })
                        .switchIfEmpty(Mono.defer(() -> {
                            Map<String, ConsentDetails> consents = new HashMap<>();
                            consents.put(tppId, ConsentDetails.builder()
                                    .tppState(true)
                                    .tcDate(LocalDateTime.now())
                                    .build());
                            CitizenConsent citizenConsentToSave = CitizenConsent.builder()
                                    .fiscalCode(fiscalCode)
                                    .consents(consents)
                                    .build();
                            return citizenRepository
                                    .save(citizenConsentToSave)
                                    .flatMap(saved -> bloomFilterService.add(fiscalCode)
                                            .thenReturn(mapperToDTO.map(saved)));
                        }))
                )
                .doOnSuccess(savedConsent ->
                    log.info("[EMD-CITIZEN][CREATE-CITIZEN-CONSENT] Created new citizen consent for fiscal code: {}", Utils.createSHA256(fiscalCode))
                );
    }

    /**
     * <p>Toggles the consent state for the specified TPP.</p>
     *
     * <p>Flow:</p>
     * <ol>
     *   <li>Log input.</li>
     *   <li>Load aggregate or error.</li>
     *   <li>Validate consent presence.</li>
     *   <li>Flip state, update timestamp, persist.</li>
     *   <li>Return DTO with only toggled consent.</li>
     * </ol>
     *
     * <p>Errors:</p>
     * <ul>
     *   <li>{@code CITIZEN_NOT_ONBOARDED} if aggregate or consent is missing.</li>
     * </ul>
     *
     * @param fiscalCode plain fiscal code
     * @param tppId TPP identifier
     * @return {@code Mono<CitizenConsentDTO>} DTO containing toggled consent
     */
    @Override
    public Mono<CitizenConsentDTO> switchState(String fiscalCode, String tppId){
        log.info("[EMD-CITIZEN][UPDATE-CHANNEL-STATE] Received hashedFiscalCode: {} and tppId: {}",
                Utils.createSHA256(fiscalCode), tppId);

        return citizenRepository.findByFiscalCode(fiscalCode)
                        .switchIfEmpty(Mono.error(exceptionMap.throwException
                                (ExceptionName.CITIZEN_NOT_ONBOARDED, "Citizen consent not founded during update state process"))
                        )
                        .flatMap(citizenConsent -> {
                            if(!citizenConsent.getConsents().containsKey(tppId))
                                return Mono.error(exceptionMap.throwException
                                        (ExceptionName.CITIZEN_NOT_ONBOARDED, "Citizen consent not founded during update state process"));
                            ConsentDetails consentDetails = citizenConsent.getConsents().get(tppId);
                            consentDetails.setTppState(!consentDetails.getTppState());
                            consentDetails.setTcDate(LocalDateTime.now());
                            return citizenRepository.save(citizenConsent)
                                    .flatMap(savedConsent -> {

                                        Map<String, ConsentDetails> consents = new HashMap<>();
                                        consents.put(tppId, citizenConsent.getConsents().get(tppId));
                                        citizenConsent.setConsents(consents);
                                        return Mono.just(mapperToDTO.map(citizenConsent));
                                    });
                        })
                        .doOnSuccess(savedConsent -> log.info("[EMD-CITIZEN][UPDATE-CHANNEL-STATE] Updated state for fiscal code: {}", Utils.createSHA256(fiscalCode)));
    }

    /**
     * <p>Retrieves consent status for a fiscal code and TPP id.</p>
     *
     * <p>Errors:</p>
     * <ul>
     *   <li>{@code CITIZEN_NOT_ONBOARDED} if consent is missing.</li>
     * </ul>
     *
     * @param fiscalCode plain fiscal code
     * @param tppId TPP identifier
     * @return {@code Mono<CitizenConsentDTO>} consent DTO
     */
    @Override
    public Mono<CitizenConsentDTO> getCitizenConsentStatus(String fiscalCode, String tppId) {
        log.info("[EMD-CITIZEN][GET-CONSENT-STATUS] Received hashedFiscalCode: {} and tppId: {}", Utils.createSHA256(fiscalCode), tppId);
        return citizenRepository.findByFiscalCodeAndTppId(fiscalCode, tppId)
                .switchIfEmpty(Mono.error(exceptionMap.throwException
                        (ExceptionName.CITIZEN_NOT_ONBOARDED, "Citizen consent not founded")))
                .map(mapperToDTO::map)
                .doOnSuccess(consent -> log.info("[EMD-CITIZEN][GET-CONSENT-STATUS] Consent consent found for fiscal code: {}", Utils.createSHA256(fiscalCode)));

    }

    /**
     * <p>Retrieves enabled TPP ids for a fiscal code (empty if citizen missing).</p>
     * <p>Flow:</p>
     * <ol>
     *   <li>Log start.</li>
     *   <li>Fetch aggregate (empty completion if absent).</li>
     *   <li>Filter enabled consents.</li>
     *   <li>Collect TPP ids.</li>
     * </ol>
     * <p>Errors:</p>
     * <ul>
     *   <li>No mapped errors; missing citizen -> empty.</li>
     * </ul>
     *
     * @param fiscalCode plain fiscal code
     * @return {@code Mono<List<String>>} list of enabled TPP ids (may be empty)
     */
    @Override
    public Mono<List<String>> getTppEnabledList(String fiscalCode) {
        log.info("[EMD-CITIZEN][FIND-CITIZEN-CONSENTS-ENABLED] Received hashedFiscalCode: {}", Utils.createSHA256(fiscalCode));

        return citizenRepository.findByFiscalCode(fiscalCode)
                .switchIfEmpty(Mono.empty())
                .map(citizenConsent -> citizenConsent.getConsents().entrySet().stream()
                        .filter(tpp -> tpp.getValue().getTppState())
                        .map(Map.Entry::getKey)
                        .toList())
                .doOnSuccess(tppIdList -> {
                    if (tppIdList != null){
                        log.info("[EMD-CITIZEN][FIND-CITIZEN-CONSENTS-ENABLED] Founded {} Consents for fiscal code: {}", tppIdList.size(),Utils.createSHA256(fiscalCode));
                    } else {
                        log.info(CONSENT_NOT_FOUND);
                    }
                });
    }

    /**
     * <p>Retrieves all consents for a fiscal code.</p>
     * <p>Flow:</p>
     * <ol>
     *   <li>Log start.</li>
     *   <li>Fetch aggregate; if absent -> error.</li>
     *   <li>Map to DTO.</li>
     * </ol>
     * <p>Errors:</p>
     * <ul>
     *   <li>Missing citizen - {@code CITIZEN_NOT_ONBOARDED}.</li>
     * </ul>
     *
     * @param fiscalCode plain fiscal code
     * @return {@code Mono<CitizenConsentDTO>} full consent DTO
     */
    @Override
    public Mono<CitizenConsentDTO> getCitizenConsentsList(String fiscalCode) {
        log.info("[EMD-CITIZEN][FIND-ALL-CITIZEN-CONSENTS] Received hashedFiscalCode: {}", (Utils.createSHA256(fiscalCode)));
        return citizenRepository.findByFiscalCode(fiscalCode)
                .switchIfEmpty(Mono.error(exceptionMap.throwException
                        (ExceptionName.CITIZEN_NOT_ONBOARDED, "Citizen consent not founded during get process ")))
                .map(mapperToDTO::map)
                .doOnSuccess(consentList -> log.info("[EMD-CITIZEN][FIND-ALL-CITIZEN-CONSENTS] Consents for fiscal code: {}", Utils.createSHA256(fiscalCode)));
    }

    /**
     * <p>Retrieves only enabled consents for a fiscal code.</p>
     * <p>Flow:</p>
     * <ol>
     *   <li>Log start.</li>
     *   <li>Fetch aggregate; if absent -> error.</li>
     *   <li>Filter enabled consents.</li>
     *   <li>Map to DTO.</li>
     * </ol>
     * <p>Errors:</p>
     * <ul>
     *   <li>Missing citizen - {@code CITIZEN_NOT_ONBOARDED}.</li>
     * </ul>
     *
     * @param fiscalCode plain fiscal code
     * @return {@code Mono<CitizenConsentDTO>} DTO with only enabled consents
     */
    @Override
    public Mono<CitizenConsentDTO> getCitizenConsentsListEnabled(String fiscalCode) {
         log.info("[EMD-CITIZEN][FIND-CITIZEN-CONSENTS-ENABLED] Received hashedFiscalCode: {}", Utils.createSHA256(fiscalCode));

        return citizenRepository.findByFiscalCode(fiscalCode)
                .switchIfEmpty(Mono.error(exceptionMap.throwException
                        (ExceptionName.CITIZEN_NOT_ONBOARDED, "Citizen consent not founded during get process ")))
                .map(citizenConsent -> {
                    Map<String, ConsentDetails> filteredConsents = citizenConsent.getConsents().entrySet().stream()
                            .filter(tpp -> tpp.getValue().getTppState())
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                    citizenConsent.setConsents(filteredConsents);

                    return mapperToDTO.map(citizenConsent);
                })
                .doOnSuccess(citizenConsent -> {
                    if (citizenConsent != null && !citizenConsent.getConsents().isEmpty()) {
                        log.info("[EMD-CITIZEN][FIND-CITIZEN-CONSENTS-ENABLED] Funded {} consents for fiscal code: {} ", citizenConsent.getConsents().size(), Utils.createSHA256(fiscalCode));
                    } else {
                        log.info(CONSENT_NOT_FOUND);
                    }
                });


    }

    /**
     * <p>Retrieves citizens with an enabled consent for a TPP id.</p>
     * <p>Flow:</p>
     * <ol>
     *   <li>Stream citizens with enabled consent for tppId.</li>
     *   <li>Map each to DTO.</li>
     *   <li>Collect into list.</li>
     * </ol>
     * <p>Errors:</p>
     * <ul>
     *   <li>Repository errors propagate.</li>
     * </ul>
     *
     * @param tppId TPP identifier
     * @return {@code Mono<List<CitizenConsentDTO>>} list (possibly empty)
     */
    @Override
    public Mono<List<CitizenConsentDTO>> getCitizenEnabled(String tppId) {
        return citizenRepository.findByTppIdEnabled(tppId)
                .map(mapperToDTO::map)
                .collectList()
                .doOnSuccess(citizenConsent -> {
                    if (citizenConsent != null && !citizenConsent.isEmpty()) {
                        log.info("[EMD-CITIZEN][FIND-CITIZEN-CONSENTS-ENABLED] Funded {} citizen for tpp: {} ", citizenConsent.size(), tppId);
                    } else {
                        log.info(CONSENT_NOT_FOUND);
                    }
                });
    }

    /**
     * <p>Deletes the citizen consent aggregate by fiscal code.</p>
     * <p>Flow:</p>
     * <ol>
     *   <li>Fetch aggregate; if absent -> error.</li>
     *   <li>Delete by id.</li>
     *   <li>Return DTO snapshot.</li>
     * </ol>
     * <p>Errors:</p>
     * <ul>
     *   <li>Missing citizen - {@code CITIZEN_NOT_ONBOARDED}.</li>
     * </ul>
     *
     * @param fiscalCode plain fiscal code
     * @return {@code Mono<CitizenConsentDTO>} deleted consent DTO
     */
    @Override
    public Mono<CitizenConsentDTO> deleteCitizenConsent(String fiscalCode) {
        return citizenRepository.findByFiscalCode(fiscalCode)
                .switchIfEmpty(Mono.error(exceptionMap.throwException
                        (ExceptionName.CITIZEN_NOT_ONBOARDED, "Citizen consent not founded during delete process ")))
                .flatMap(citizenConsent ->
                        citizenRepository.deleteById(citizenConsent.getId())
                                .then(Mono.just(mapperToDTO.map(citizenConsent)))
                );
    }

    /**
     * <p>Checks if a fiscal code is in the Bloom filter and has at least one enabled consent with an active TPP.</p>
     * <p>Flow:</p>
     * <ol>
     *   <li>Check Bloom filter membership (false if absent).</li>
     *   <li>If present, query DB for citizen consents.</li>
     *   <li>Filter consents with tppState = true.</li>
     *   <li>Call TPP service to verify which TPPs are still active.</li>
     *   <li>Return true if at least one active TPP exists.</li>
     * </ol>
     * <p>Errors:</p>
     * <ul>
     *   <li>Repository and TPP connector errors propagate.</li>
     * </ul>
     *
     * @param fiscalCode plain fiscal code
     * @return {@code Mono<Boolean>} {@code true} if present in Bloom filter and at least one enabled consent with active TPP exists
     */
    @Override
    public Mono<Boolean> getCitizenInBloomFilter(String fiscalCode) {
        String hashedFiscalCode = Utils.createSHA256(fiscalCode);
        log.info("[EMD-CITIZEN][BLOOM-FILTER-SEARCH] Start search for hashedFiscalCode: {}", hashedFiscalCode);
        return bloomFilterService.contains(fiscalCode)
                .flatMap(isPresent -> {
                    if (!isPresent) {
                        log.info("[EMD-CITIZEN][BLOOM-FILTER-SEARCH] Fiscal Code {} NOT found in bloom filter", hashedFiscalCode);
                        return Mono.just(false);
                    }
                    log.info("[EMD-CITIZEN][BLOOM-FILTER-SEARCH] Fiscal Code {} found in bloom filter. Checking consents in DB...", hashedFiscalCode);

                    return citizenRepository.findByFiscalCode(fiscalCode)
                        .flatMap(citizenConsent -> {
                            List<String> list = citizenConsent.getConsents().entrySet().stream()
                                    .filter(tpp -> tpp.getValue().getTppState())
                                    .map(Map.Entry::getKey)
                                    .toList();
                            // check for tpp with state = true
                            return tppConnector.getTppsEnabled(new TppIdList(list));
                        })
                        .map(listTpp -> !listTpp.isEmpty())
                        .defaultIfEmpty(false)
                        .doOnSuccess(hasActiveConsent -> {
                            if (hasActiveConsent){
                                log.info("[EMD-CITIZEN][BLOOM-FILTER-SEARCH] Found consents for fiscal code: {}", hashedFiscalCode);
                            } else {
                                log.info("[EMD-CITIZEN][BLOOM-FILTER-SEARCH] No consents enabled found for Fiscal Code {}", hashedFiscalCode);
                            }
                        });
                });
    }

}
