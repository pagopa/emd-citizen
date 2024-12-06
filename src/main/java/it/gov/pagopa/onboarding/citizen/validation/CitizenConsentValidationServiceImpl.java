package it.gov.pagopa.onboarding.citizen.validation;

import it.gov.pagopa.common.utils.Utils;
import it.gov.pagopa.onboarding.citizen.configuration.ExceptionMap;
import it.gov.pagopa.onboarding.citizen.connector.tpp.TppConnectorImpl;
import it.gov.pagopa.onboarding.citizen.constants.CitizenConstants;
import it.gov.pagopa.onboarding.citizen.constants.CitizenConstants.ExceptionName;
import it.gov.pagopa.onboarding.citizen.dto.CitizenConsentDTO;
import it.gov.pagopa.onboarding.citizen.dto.mapper.CitizenConsentObjectToDTOMapper;
import it.gov.pagopa.onboarding.citizen.model.CitizenConsent;
import it.gov.pagopa.onboarding.citizen.repository.CitizenRepository;
import it.gov.pagopa.onboarding.citizen.service.BloomFilterServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class CitizenConsentValidationServiceImpl implements CitizenConsentValidationService {

    private final CitizenRepository citizenRepository;

    private final BloomFilterServiceImpl bloomFilterService;

    private final TppConnectorImpl tppConnector;
    private final CitizenConsentObjectToDTOMapper mapperToDTO;
    private final ExceptionMap exceptionMap;

    public CitizenConsentValidationServiceImpl(CitizenRepository citizenRepository, BloomFilterServiceImpl bloomFilterService, TppConnectorImpl tppConnector,
                                               CitizenConsentObjectToDTOMapper mapperToDTO, ExceptionMap exceptionMap) {
        this.citizenRepository = citizenRepository;
        this.bloomFilterService = bloomFilterService;
        this.tppConnector = tppConnector;
        this.mapperToDTO = mapperToDTO;
        this.exceptionMap = exceptionMap;
    }

    @Override
    public Mono<CitizenConsentDTO> handleExistingConsent(CitizenConsent existingConsent, String tppId, CitizenConsent citizenConsent) {
        if (existingConsent.getConsents().containsKey(tppId)) {
            return Mono.just(mapperToDTO.map(citizenConsent));
        } else {
            return validateTppAndUpdateConsent(existingConsent, tppId, citizenConsent);
        }
    }

    @Override
    public Mono<CitizenConsentDTO> validateTppAndSaveConsent(String fiscalCode, String tppId, CitizenConsent citizenConsent) {
        return tppConnector.get(tppId)
                .onErrorMap(error -> exceptionMap.throwException(ExceptionName.TPP_NOT_FOUND, CitizenConstants.ExceptionMessage.TPP_NOT_FOUND))
                .flatMap(tppResponse -> {
                    if (Boolean.TRUE.equals(citizenConsent.getConsents().get(tppId).getTppState())) {
                        return citizenRepository.save(citizenConsent)
                                .doOnSuccess(savedConsent -> {
                                    log.info("[EMD][CREATE-CITIZEN-CONSENT] Created new citizen consent for fiscal code: {}", Utils.createSHA256(fiscalCode));
                                    bloomFilterService.add(fiscalCode);
                                })
                                .map(savedConsent -> mapperToDTO.map(citizenConsent));
                    } else {
                        return Mono.error(exceptionMap.throwException(ExceptionName.TPP_NOT_FOUND, "TPP is not active or is invalid"));
                    }
                });
    }


    private Mono<CitizenConsentDTO> validateTppAndUpdateConsent(CitizenConsent existingConsent, String tppId, CitizenConsent citizenConsent) {
        return tppConnector.get(tppId)
                .onErrorMap(error -> exceptionMap.throwException(ExceptionName.TPP_NOT_FOUND, CitizenConstants.ExceptionMessage.TPP_NOT_FOUND))
                .flatMap(tppResponse -> {
                        existingConsent.getConsents().put(tppId, citizenConsent.getConsents().get(tppId));
                        return citizenRepository.save(existingConsent)
                                .doOnSuccess(savedConsent -> log.info("[EMD][CREATE-CITIZEN-CONSENT] Updated citizen consent for TPP: {}", tppId))
                                .map(savedConsent -> mapperToDTO.map(citizenConsent));
                });
    }
}
