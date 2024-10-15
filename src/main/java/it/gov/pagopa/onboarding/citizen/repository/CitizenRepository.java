package it.gov.pagopa.onboarding.citizen.repository;

import it.gov.pagopa.onboarding.citizen.model.CitizenConsent;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CitizenRepository extends ReactiveMongoRepository<CitizenConsent, String> {

    Flux<CitizenConsent> findByHashedFiscalCode(String hashedFiscalCode);

    Flux<CitizenConsent> findByHashedFiscalCodeAndTppStateTrue(String hashedFiscalCode);

    Mono<CitizenConsent> findByHashedFiscalCodeAndTppId(String hashedFiscalCode, String tppId);

}
