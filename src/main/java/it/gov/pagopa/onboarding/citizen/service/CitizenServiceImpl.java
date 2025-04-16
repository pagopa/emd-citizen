package it.gov.pagopa.onboarding.citizen.service;

import it.gov.pagopa.common.utils.Utils;
import it.gov.pagopa.onboarding.citizen.configuration.ExceptionMap;
import it.gov.pagopa.onboarding.citizen.connector.tpp.TppConnectorImpl;
import it.gov.pagopa.onboarding.citizen.constants.CitizenConstants.ExceptionMessage;
import it.gov.pagopa.onboarding.citizen.constants.CitizenConstants.ExceptionName;
import it.gov.pagopa.onboarding.citizen.dto.CitizenConsentDTO;
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
                                    .map(mapperToDTO::map)
                                    .doOnSuccess(citizenConsentSaved -> bloomFilterService.add(fiscalCode));
                        }))
                )
                .doOnSuccess(savedConsent ->
                    log.info("[EMD-CITIZEN][CREATE-CITIZEN-CONSENT] Created new citizen consent for fiscal code: {}", Utils.createSHA256(fiscalCode))
                );
    }
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

    @Override
    public Mono<CitizenConsentDTO> getCitizenConsentStatus(String fiscalCode, String tppId) {
        log.info("[EMD-CITIZEN][GET-CONSENT-STATUS] Received hashedFiscalCode: {} and tppId: {}", Utils.createSHA256(fiscalCode), tppId);
        return citizenRepository.findByFiscalCodeAndTppId(fiscalCode, tppId)
                .switchIfEmpty(Mono.error(exceptionMap.throwException
                        (ExceptionName.CITIZEN_NOT_ONBOARDED, "Citizen consent not founded")))
                .map(mapperToDTO::map)
                .doOnSuccess(consent -> log.info("[EMD-CITIZEN][GET-CONSENT-STATUS] Consent consent found for fiscal code: {}", Utils.createSHA256(fiscalCode)));
      
    }

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

    @Override
    public Mono<CitizenConsentDTO> getCitizenConsentsList(String fiscalCode) {
        log.info("[EMD-CITIZEN][FIND-ALL-CITIZEN-CONSENTS] Received hashedFiscalCode: {}", (Utils.createSHA256(fiscalCode)));
        return citizenRepository.findByFiscalCode(fiscalCode)
                .switchIfEmpty(Mono.error(exceptionMap.throwException
                        (ExceptionName.CITIZEN_NOT_ONBOARDED, "Citizen consent not founded during get process ")))
                .map(mapperToDTO::map)
                .doOnSuccess(consentList -> log.info("[EMD-CITIZEN][FIND-ALL-CITIZEN-CONSENTS] Consents for fiscal code: {}", Utils.createSHA256(fiscalCode)));
    }

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

}
