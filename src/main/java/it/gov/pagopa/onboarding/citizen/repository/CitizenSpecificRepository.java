package it.gov.pagopa.onboarding.citizen.repository;

import it.gov.pagopa.onboarding.citizen.model.CitizenConsent;
import reactor.core.publisher.Mono;

import java.util.List;

public interface CitizenSpecificRepository {
    Mono<List<String>> findByFiscalCodeAndTppStateTrue(String fiscalCode);
    Mono<CitizenConsent> findByFiscalCodeAndTppId(String fiscalCode, String tppId);
}
