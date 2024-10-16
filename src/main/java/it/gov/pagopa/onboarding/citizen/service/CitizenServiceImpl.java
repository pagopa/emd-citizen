package it.gov.pagopa.onboarding.citizen.service;

import it.gov.pagopa.common.utils.Utils;
import it.gov.pagopa.onboarding.citizen.constants.OnboardingCitizenConstants.ExceptionName;
import it.gov.pagopa.onboarding.citizen.dto.CitizenConsentDTO;
import it.gov.pagopa.onboarding.citizen.dto.mapper.CitizenConsentObjectToDTOMapper;
import it.gov.pagopa.onboarding.citizen.configuration.ExceptionMap;
import it.gov.pagopa.onboarding.citizen.model.CitizenConsent;
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

    public CitizenServiceImpl(CitizenRepository citizenRepository, CitizenConsentObjectToDTOMapper mapperToDTO, CitizenConsentDTOToObjectMapper mapperToObject, ExceptionMap exceptionMap) {
        this.citizenRepository = citizenRepository;
        this.mapperToDTO = mapperToDTO;
        this.mapperToObject = mapperToObject;
        this.exceptionMap = exceptionMap;
    }

    @Override
    public Mono<CitizenConsentDTO> createCitizenConsent(CitizenConsentDTO citizenConsentDTO) {
        log.info("[EMD][CITIZEN][CREATE] Received message: {}",inputSanify(citizenConsentDTO.toString()));
        CitizenConsent citizenConsent = mapperToObject.map(citizenConsentDTO);
        String hashedFiscalCode = Utils.createSHA256(citizenConsent.getHashedFiscalCode());
        citizenConsent.setHashedFiscalCode(hashedFiscalCode);
        citizenConsent.setCreationDate(LocalDateTime.now());
        citizenConsent.setLastUpdateDate(LocalDateTime.now());
        return citizenRepository.save(citizenConsent)
                .map(mapperToDTO::map)
                .doOnSuccess(savedConsent -> log.info("[EMD][CREATE-CITIZEN-CONSENT] Created"));

    }

    @Override
    public Mono<CitizenConsentDTO> updateChannelState(String fiscalCode, String tppId, boolean tppState) {
        String hashedFiscalCode = Utils.createSHA256(fiscalCode);
        log.info("[EMD][[CITIZEN][UPDATE-CHANNEL-STATE] Received hashedFiscalCode: {} and tppId: {} with state: {}"
                ,hashedFiscalCode, inputSanify(tppId), tppState);
        return citizenRepository.findByHashedFiscalCodeAndTppId(hashedFiscalCode, tppId)
                .switchIfEmpty(Mono.error(exceptionMap.getException(ExceptionName.CITIZEN_NOT_ONBOARDED)))
                .flatMap(citizenConsent -> {
                    citizenConsent.setTppState(tppState);
                    citizenConsent.setLastUpdateDate(LocalDateTime.now());
                    return citizenRepository.save(citizenConsent);
                })
                .map(mapperToDTO::map)
                .doOnSuccess(savedConsent -> log.info("[EMD][[CITIZEN][UPDATE-CHANNEL-STATE] Updated state"));
    }

    @Override
    public Mono<CitizenConsentDTO> getConsentStatus(String fiscalCode, String tppId) {
        String hashedFiscalCode = Utils.createSHA256(fiscalCode);
        log.info("[EMD][CITIZEN][GET-CONSENT-STATUS] Received hashedFiscalCode: {} and tppId: {}",hashedFiscalCode,inputSanify(tppId));
        return citizenRepository.findByHashedFiscalCodeAndTppId(hashedFiscalCode, tppId)
                .switchIfEmpty(Mono.error(exceptionMap.getException(ExceptionName.CITIZEN_NOT_ONBOARDED)))
                .map(mapperToDTO::map)
                .doOnSuccess(consent -> log.info("[EMD][CITIZEN][GET-CONSENT-STATUS] Consent found::  {}",consent));

    }

    @Override
    public Mono<List<CitizenConsentDTO>> getListEnabledConsents(String fiscalCode) {
        String hashedFiscalCode = Utils.createSHA256(fiscalCode);
        log.info("[EMD][CITIZEN][FIND-CITIZEN-CONSENTS-ENABLED] Received hashedFiscalCode: {}",hashedFiscalCode);
        return citizenRepository.findByHashedFiscalCodeAndTppStateTrue(hashedFiscalCode)
                .collectList()
                .map(consentList -> consentList.stream()
                        .map(mapperToDTO::map)
                        .toList()
                )
                .doOnSuccess(consentList -> log.info("EMD][CITIZEN][FIND-CITIZEN-CONSENTS-ENABLED] Consents founded:  {}",(consentList.size())));

    }

    @Override
    public Mono<List<CitizenConsentDTO>> getListAllConsents(String fiscalCode) {
        String hashedFiscalCode = Utils.createSHA256(fiscalCode);
        log.info("[EMD][CITIZEN][FIND-ALL-CITIZEN-CONSENTS] Received hashedFiscalCode: {}",(hashedFiscalCode));
        return citizenRepository.findByHashedFiscalCode(hashedFiscalCode)
                .collectList()
                .map(consentList -> consentList.stream()
                        .map(mapperToDTO::map)
                        .toList()
                )
                .doOnSuccess(consentList -> log.info("[EMD][CITIZEN][FIND-ALL-CITIZEN-CONSENTS] Consents found::  {}",consentList));
    }
}
