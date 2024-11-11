package it.gov.pagopa.onboarding.citizen.repository;

import it.gov.pagopa.onboarding.citizen.model.CitizenConsent;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CitizenRepository extends ReactiveMongoRepository<CitizenConsent, String>, CitizenSpecificRepository {

    Mono<CitizenConsent> findByFiscalCode(String fiscalCode);

    @Query(value = "{}", fields = "{ 'fiscalCode' : 1, '_id' : 0 }")
    Flux<String> findAllFiscalCodes();

}
