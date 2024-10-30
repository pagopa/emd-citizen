package it.gov.pagopa.onboarding.citizen.repository;

import it.gov.pagopa.onboarding.citizen.model.CitizenConsent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CitizenSpecificRepository {
    Flux<CitizenConsent> findByHashedFiscalCodeAndTppStateTrue(String hashedFiscalCode);
    Mono<CitizenConsent> findByHashedFiscalCodeAndTppId(String hashedFiscalCode, String tppId);
}
