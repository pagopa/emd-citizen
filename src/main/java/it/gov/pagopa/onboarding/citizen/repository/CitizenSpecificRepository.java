package it.gov.pagopa.onboarding.citizen.repository;

import it.gov.pagopa.onboarding.citizen.model.CitizenConsent;
import reactor.core.publisher.Mono;


public interface CitizenSpecificRepository {

    Mono<CitizenConsent> findByFiscalCodeAndTppId(String fiscalCode, String tppId);
}
