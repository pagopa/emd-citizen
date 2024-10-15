package it.gov.pagopa.onboarding.citizen.service;

import it.gov.pagopa.common.utils.Utils;
import it.gov.pagopa.onboarding.citizen.constants.OnboardingCitizenConstants.ExceptionName;
import it.gov.pagopa.onboarding.citizen.dto.CitizenConsentDTO;
import it.gov.pagopa.onboarding.citizen.dto.mapper.CitizenConsentMapperToDTO;
import it.gov.pagopa.onboarding.citizen.exception.custom.ExceptionMap;
import it.gov.pagopa.onboarding.citizen.model.CitizenConsent;
import it.gov.pagopa.onboarding.citizen.model.mapper.CitizenConsentMapperToObject;
import it.gov.pagopa.onboarding.citizen.repository.CitizenRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static it.gov.pagopa.common.utils.Utils.logInfo;

@Service
public class CitizenServiceImpl implements CitizenService {

    private final CitizenRepository citizenRepository;
    private final CitizenConsentMapperToDTO mapperToDTO;
    private final CitizenConsentMapperToObject mapperToObject;
    private final ExceptionMap exceptionMap;

    public CitizenServiceImpl(CitizenRepository citizenRepository, CitizenConsentMapperToDTO mapperToDTO, CitizenConsentMapperToObject mapperToObject, ExceptionMap exceptionMap) {
        this.citizenRepository = citizenRepository;
        this.mapperToDTO = mapperToDTO;
        this.mapperToObject = mapperToObject;
        this.exceptionMap = exceptionMap;
    }

    @Override
    public Mono<CitizenConsentDTO> createCitizenConsent(CitizenConsentDTO citizenConsentDTO) {
        logInfo("[EMD][CREATE-CITIZEN-CONSENT] Received message: %s".formatted(citizenConsentDTO.toString()));
        CitizenConsent citizenConsent = mapperToObject.citizenConsentDTOMapper(citizenConsentDTO);
        String hashedFiscalCode = Utils.createSHA256(citizenConsent.getHashedFiscalCode());
        citizenConsent.setHashedFiscalCode(hashedFiscalCode);
        citizenConsent.setCreationDate(LocalDateTime.now());
        citizenConsent.setLastUpdateDate(LocalDateTime.now());

        return citizenRepository.save(citizenConsent)
                .doOnSuccess(savedConsent -> logInfo("[EMD][CREATE-CITIZEN-CONSENT] Created"))
                .map(mapperToDTO::citizenConsentMapper);
    }

    @Override
    public Mono<CitizenConsentDTO> updateChannelState(String hashedFiscalCode, String tppId, boolean tppState) {
        logInfo("[EMD][UPDATE-CHANNEL-STATE] Received hashedFiscalCode: %s and tppId: %s with state: %s".formatted(hashedFiscalCode, tppId, tppState));

        return citizenRepository.findByHashedFiscalCodeAndTppId(hashedFiscalCode, tppId)
                .switchIfEmpty(Mono.error(exceptionMap.getException(ExceptionName.CITIZEN_NOT_ONBOARDED)))
                .flatMap(citizenConsent -> {
                    citizenConsent.setTppState(tppState);
                    citizenConsent.setLastUpdateDate(LocalDateTime.now());
                    return citizenRepository.save(citizenConsent);
                })
                .doOnSuccess(savedConsent -> logInfo("[EMD][UPDATE-CHANNEL-STATE] Updated state"))
                .map(mapperToDTO::citizenConsentMapper);
    }

    @Override
    public Mono<CitizenConsentDTO> getConsentStatus(String fiscalCode, String tppId) {
        logInfo("[EMD][GET-CONSENT-STATUS] Received fiscalCode: %s and tppId: %s".formatted(fiscalCode, tppId));
        String hashedFiscalCode = Utils.createSHA256(fiscalCode);

        return citizenRepository.findByHashedFiscalCodeAndTppId(hashedFiscalCode, tppId)
                .switchIfEmpty(Mono.error(exceptionMap.getException(ExceptionName.CITIZEN_NOT_ONBOARDED)))
                .map(mapperToDTO::citizenConsentMapper);
    }

    @Override
    public Flux<CitizenConsentDTO> getListEnabledConsents(String fiscalCode) {
        logInfo("[EMD][FIND-CITIZEN-CONSENTS-ENABLED] Received fiscalCode: %s".formatted(fiscalCode));
        String hashedFiscalCode = Utils.createSHA256(fiscalCode);

        return citizenRepository.findByHashedFiscalCodeAndTppStateTrue(hashedFiscalCode)
                .map(mapperToDTO::citizenConsentMapper)
                .doOnNext(citizenConsentDTO -> logInfo("[EMD][FIND-CITIZEN-CONSENT-ENABLED] Consents enabled found: %s".formatted(citizenConsentDTO)));
    }

    @Override
    public Flux<CitizenConsentDTO> getListAllConsents(String fiscalCode) {
        logInfo("[EMD][FIND-ALL-CITIZEN-CONSENTS] Received fiscalCode: %s".formatted(fiscalCode));
        String hashedFiscalCode = Utils.createSHA256(fiscalCode);

        return citizenRepository.findByHashedFiscalCode(hashedFiscalCode)
                .map(mapperToDTO::citizenConsentMapper)
                .doOnNext(citizenConsentDTO -> logInfo("[EMD][FIND-ALL-CITIZEN-CONSENTS] Consents found: %s".formatted(citizenConsentDTO)));
    }
}
