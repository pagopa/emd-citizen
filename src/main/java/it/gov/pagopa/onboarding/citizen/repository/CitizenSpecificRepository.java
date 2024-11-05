package it.gov.pagopa.onboarding.citizen.repository;

import it.gov.pagopa.onboarding.citizen.model.CitizenConsent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CitizenSpecificRepository {
    Flux<CitizenConsent> findByFiscalCodeAndTppStateTrue(String fiscalCode);
    Mono<CitizenConsent> findByFiscalCodeAndTppId(String fiscalCode, String tppId);
}
