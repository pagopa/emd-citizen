package it.gov.pagopa.onboarding.citizen.validation;

import it.gov.pagopa.onboarding.citizen.dto.CitizenConsentDTO;
import it.gov.pagopa.onboarding.citizen.model.CitizenConsent;
import reactor.core.publisher.Mono;

public interface CitizenConsentValidationService {

    Mono<CitizenConsentDTO> handleExistingConsent(CitizenConsent existingConsent, String tppId, CitizenConsent citizenConsent);

    Mono<CitizenConsentDTO> validateTppAndSaveConsent(String fiscalCode, String tppId, CitizenConsent citizenConsent);

}
