package it.gov.pagopa.onboarding.citizen.service;

import it.gov.pagopa.common.utils.Utils;
import it.gov.pagopa.onboarding.citizen.configuration.ExceptionMap;
import it.gov.pagopa.onboarding.citizen.connector.tpp.TppConnectorImpl;
import it.gov.pagopa.onboarding.citizen.constants.CitizenConstants.ExceptionName;
import it.gov.pagopa.onboarding.citizen.dto.CitizenConsentDTO;
import it.gov.pagopa.onboarding.citizen.dto.mapper.CitizenConsentObjectToDTOMapper;
import it.gov.pagopa.onboarding.citizen.model.CitizenConsent;
import it.gov.pagopa.onboarding.citizen.model.ConsentDetails;
import it.gov.pagopa.onboarding.citizen.model.mapper.CitizenConsentDTOToObjectMapper;
import it.gov.pagopa.onboarding.citizen.repository.CitizenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

import static it.gov.pagopa.common.utils.Utils.inputSanify;

@Service
@Slf4j
public class CitizenServiceImpl implements CitizenService {

    private final CitizenRepository citizenRepository;
    private final CitizenConsentObjectToDTOMapper mapperToDTO;
    private final CitizenConsentDTOToObjectMapper mapperToObject;
    private final ExceptionMap exceptionMap;
    private final TppConnectorImpl tppConnector;

    public CitizenServiceImpl(CitizenRepository citizenRepository, CitizenConsentObjectToDTOMapper mapperToDTO, CitizenConsentDTOToObjectMapper mapperToObject, ExceptionMap exceptionMap, TppConnectorImpl tppConnector) {
        this.citizenRepository = citizenRepository;
        this.mapperToDTO = mapperToDTO;
        this.mapperToObject = mapperToObject;
        this.exceptionMap = exceptionMap;
        this.tppConnector = tppConnector;
    }

    @Override
    public Mono<CitizenConsentDTO> createCitizenConsent(CitizenConsentDTO citizenConsentDTO) {
        CitizenConsent citizenConsent = mapperToObject.map(citizenConsentDTO);
        String fiscalCode = citizenConsent.getFiscalCode();

        citizenConsent.getConsents().forEach((tppId, consentDetails) -> consentDetails.setTcDate(LocalDateTime.now()));

        log.info("[EMD-CITIZEN][CREATE] Received consent: {}", inputSanify(citizenConsent.toString()));

        String tppId = citizenConsent.getConsents().keySet().stream().findFirst().orElse(null);
        if (tppId == null) {
            return Mono.error(exceptionMap.throwException(ExceptionName.TPP_NOT_FOUND, "TPP does not exist or is not active"));
        }

        return citizenRepository.findByFiscalCodeAndTppId(fiscalCode, tppId)
                .flatMap(existingConsent -> {
                    log.info("[EMD][CREATE-CITIZEN-CONSENT] Citizen consent already exists");
                    return Mono.just(mapperToDTO.map(existingConsent));
                })
                .switchIfEmpty(tppConnector.get(tppId)
                        .flatMap(tppResponse -> {
                            if (tppResponse != null && Boolean.TRUE.equals(tppResponse.getState())) {
                                return citizenRepository.save(citizenConsent)
                                        .doOnSuccess(savedConsent -> log.info("[EMD][CREATE-CITIZEN-CONSENT] Created new citizen consent"))
                                        .flatMap(savedConsent -> Mono.just(mapperToDTO.map(savedConsent)));
                            } else {
                                return Mono.error(exceptionMap.throwException(ExceptionName.TPP_NOT_FOUND, "TPP does not exist or is not active"));
                            }
                        })
                );
    }

    @Override
    public Mono<CitizenConsentDTO> updateChannelState(String fiscalCode, String tppId, boolean tppState) {
        log.info("[EMD][CITIZEN][UPDATE-CHANNEL-STATE] Received hashedFiscalCode: {} and tppId: {} with state: {}",
                Utils.createSHA256(fiscalCode), inputSanify(tppId), tppState);

        return tppConnector.get(tppId)
                .switchIfEmpty(Mono.error(exceptionMap.throwException(ExceptionName.TPP_NOT_FOUND, "TPP does not exist or is not active")))
                .flatMap(tppResponse -> {
                    if (tppResponse == null || !Boolean.TRUE.equals(tppResponse.getState())) {
                        return Mono.error(exceptionMap.throwException(ExceptionName.TPP_NOT_FOUND, "TPP does not exist or is not active"));
                    }
                    return citizenRepository.findByFiscalCodeAndTppId(fiscalCode, tppId)
                            .switchIfEmpty(Mono.error(exceptionMap.throwException
                                    (ExceptionName.CITIZEN_NOT_ONBOARDED, "Citizen consent not founded during update state process")))
                            .flatMap(citizenConsent -> {
                                ConsentDetails consentDetails = citizenConsent.getConsents().get(tppId);
                                if (consentDetails != null) {
                                    consentDetails.setTppState(tppState);
                                } else {
                                    return Mono.error(exceptionMap.throwException
                                            (ExceptionName.CITIZEN_NOT_ONBOARDED, "ConsentDetails is null for this tppId"));
                                }
                                return citizenRepository.save(citizenConsent);
                            })
                            .map(mapperToDTO::map)
                            .doOnSuccess(savedConsent -> log.info("[EMD][CITIZEN][UPDATE-CHANNEL-STATE] Updated state"));
                });
    }

    @Override
    public Mono<CitizenConsentDTO> getConsentStatus(String fiscalCode, String tppId) {
        log.info("[EMD-CITIZEN][GET-CONSENT-STATUS] Received hashedFiscalCode: {} and tppId: {}", Utils.createSHA256(fiscalCode), inputSanify(tppId));
        return citizenRepository.findByFiscalCodeAndTppId(fiscalCode, tppId)
                .switchIfEmpty(Mono.error(exceptionMap.throwException
                        (ExceptionName.CITIZEN_NOT_ONBOARDED, "Citizen consent not founded during get process ")))
                .map(mapperToDTO::map)
                .doOnSuccess(consent -> log.info("[EMD-CITIZEN][GET-CONSENT-STATUS] Consent consent found::  {}", consent));

    }

    @Override
    public Mono<List<CitizenConsentDTO>> getListEnabledConsents(String fiscalCode) {
        log.info("[EMD-CITIZEN][FIND-CITIZEN-CONSENTS-ENABLED] Received hashedFiscalCode: {}", Utils.createSHA256(fiscalCode));
        return citizenRepository.findByFiscalCodeAndTppStateTrue(fiscalCode)
                .collectList()
                .map(consentList -> consentList.stream()
                        .map(mapperToDTO::map)
                        .toList()
                )
                .doOnSuccess(consentList -> log.info("EMD][CITIZEN][FIND-CITIZEN-CONSENTS-ENABLED] Consents founded:  {}", (consentList.size())));

    }

    @Override
    public Mono<List<CitizenConsentDTO>> getListAllConsents(String fiscalCode) {
        log.info("[EMD-CITIZEN][FIND-ALL-CITIZEN-CONSENTS] Received hashedFiscalCode: {}", (Utils.createSHA256(fiscalCode)));
        return citizenRepository.findByFiscalCode(fiscalCode)
                .collectList()
                .map(consentList -> consentList.stream()
                        .map(mapperToDTO::map)
                        .toList()
                )
                .doOnSuccess(consentList -> log.info("[EMD-CITIZEN][FIND-ALL-CITIZEN-CONSENTS] Consents found::  {}", consentList));
    }
}
