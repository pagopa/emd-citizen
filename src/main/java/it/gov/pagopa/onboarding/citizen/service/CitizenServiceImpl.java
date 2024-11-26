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
import it.gov.pagopa.onboarding.citizen.model.mapper.CitizenConsentDTOToObjectMapper;
import it.gov.pagopa.onboarding.citizen.repository.CitizenRepository;
import it.gov.pagopa.onboarding.citizen.validation.CitizenConsentValidationServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static it.gov.pagopa.common.utils.Utils.inputSanify;

@Service
@Slf4j
public class CitizenServiceImpl implements CitizenService {

    private final CitizenRepository citizenRepository;
    private final CitizenConsentObjectToDTOMapper mapperToDTO;
    private final CitizenConsentDTOToObjectMapper mapperToObject;
    private final ExceptionMap exceptionMap;
    private final TppConnectorImpl tppConnector;
    private final CitizenConsentValidationServiceImpl validationService;

    public CitizenServiceImpl(CitizenRepository citizenRepository, CitizenConsentObjectToDTOMapper mapperToDTO, CitizenConsentDTOToObjectMapper mapperToObject, ExceptionMap exceptionMap, TppConnectorImpl tppConnector, CitizenConsentValidationServiceImpl validationService) {
        this.citizenRepository = citizenRepository;
        this.mapperToDTO = mapperToDTO;
        this.mapperToObject = mapperToObject;
        this.exceptionMap = exceptionMap;
        this.tppConnector = tppConnector;
        this.validationService = validationService;
    }

    @Override
    public Mono<CitizenConsentDTO> createCitizenConsent(CitizenConsentDTO citizenConsentDTO) {

        CitizenConsent citizenConsent = mapperToObject.map(citizenConsentDTO);
        String fiscalCode = citizenConsent.getFiscalCode();

        citizenConsent.getConsents().forEach((tppId, consentDetails) -> consentDetails.setTcDate(LocalDateTime.now()));

        log.info("[EMD-CITIZEN][CREATE] Received consent: {}", inputSanify(citizenConsent.toString()));

        String tppId = citizenConsent.getConsents().keySet().stream().findFirst().orElse(null);
        if (tppId == null) {
            return Mono.error(exceptionMap.throwException(ExceptionName.TPP_NOT_FOUND, ExceptionMessage.TPP_NOT_FOUND));
        }

        return citizenRepository.findByFiscalCode(fiscalCode)
                .flatMap(existingConsent -> validationService.handleExistingConsent(existingConsent, tppId, citizenConsent))
                .switchIfEmpty(validationService.validateTppAndSaveConsent(fiscalCode, tppId, citizenConsent));
    }

    @Override
    public Mono<CitizenConsentDTO> updateTppState(String fiscalCode, String tppId, boolean tppState) {
        log.info("[EMD][CITIZEN][UPDATE-CHANNEL-STATE] Received hashedFiscalCode: {} and tppId: {} with state: {}",
                Utils.createSHA256(fiscalCode), inputSanify(tppId), tppState);

        return tppConnector.get(tppId)
                .onErrorMap(error ->exceptionMap.throwException(ExceptionName.TPP_NOT_FOUND, ExceptionMessage.TPP_NOT_FOUND))
                .flatMap(tppResponse -> citizenRepository.findByFiscalCodeAndTppId(fiscalCode, tppId)
                        .switchIfEmpty(Mono.error(exceptionMap.throwException
                                (ExceptionName.CITIZEN_NOT_ONBOARDED, "Citizen consent not founded during update state process")))
                        .flatMap(citizenConsent -> {
                            ConsentDetails consentDetails = citizenConsent.getConsents().get(tppId);
                            consentDetails.setTppState(tppState);
                            return citizenRepository.save(citizenConsent);
                        })
                        .map(mapperToDTO::map)
                        .doOnSuccess(savedConsent -> log.info("[EMD][CITIZEN][UPDATE-CHANNEL-STATE] Updated state")));
    }

    @Override
    public Mono<CitizenConsentDTO> getCitizenConsentStatus(String fiscalCode, String tppId) {
        log.info("[EMD-CITIZEN][GET-CONSENT-STATUS] Received hashedFiscalCode: {} and tppId: {}", Utils.createSHA256(fiscalCode), inputSanify(tppId));
        return citizenRepository.findByFiscalCodeAndTppId(fiscalCode, tppId)
                .switchIfEmpty(Mono.error(exceptionMap.throwException
                        (ExceptionName.CITIZEN_NOT_ONBOARDED, "Citizen consent not founded during get process ")))
                .map(mapperToDTO::map)
                .doOnSuccess(consent -> log.info("[EMD-CITIZEN][GET-CONSENT-STATUS] Consent consent found::  {}", consent));

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
                .doOnSuccess(tppIdList -> log.info("EMD][CITIZEN][FIND-CITIZEN-CONSENTS-ENABLED] Consents found:  {}", (tppIdList.size())));
    }

    @Override
    public Mono<CitizenConsentDTO> getCitizenConsentsList(String fiscalCode) {
        log.info("[EMD-CITIZEN][FIND-ALL-CITIZEN-CONSENTS] Received hashedFiscalCode: {}", (Utils.createSHA256(fiscalCode)));
        return citizenRepository.findByFiscalCode(fiscalCode)
                .map(mapperToDTO::map)
                .doOnSuccess(consentList -> log.info("[EMD-CITIZEN][FIND-ALL-CITIZEN-CONSENTS] Consents found:  {}", consentList));
    }

    @Override
    public Mono<CitizenConsentDTO> getCitizenConsentsListEnabled(String fiscalCode) {
         log.info("[EMD-CITIZEN][FIND-CITIZEN-CONSENTS-ENABLED] Received hashedFiscalCode: {}", Utils.createSHA256(fiscalCode));

        return citizenRepository.findByFiscalCode(fiscalCode)
                .switchIfEmpty(Mono.empty())
                .map(citizenConsent -> {
                    Map<String, ConsentDetails> filteredConsents = citizenConsent.getConsents().entrySet().stream()
                            .filter(tpp -> tpp.getValue().getTppState())
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                    citizenConsent.setConsents(filteredConsents);

                    return mapperToDTO.map(citizenConsent);
                })
                .doOnSuccess(citizenConsent -> log.info("EMD][CITIZEN][FIND-CITIZEN-CONSENTS-ENABLED] Consents found:  {}", citizenConsent.getConsents().size()));


    }

    @Override
    public Mono<List<CitizenConsentDTO>> getCitizenEnabled(String tppId) {
        return citizenRepository.findByTppIdEnabled(tppId)
                .map(mapperToDTO::map)
                .collectList();
    }
}
