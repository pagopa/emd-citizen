package it.gov.pagopa.onboarding.citizen.repository;

import it.gov.pagopa.onboarding.citizen.model.CitizenConsent;
import it.gov.pagopa.onboarding.citizen.model.ConsentDetails;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class CitizenSpecificRepositoryImpl implements CitizenSpecificRepository {

    private final ReactiveMongoTemplate mongoTemplate;

    public CitizenSpecificRepositoryImpl(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public Flux<CitizenConsent> findByHashedFiscalCodeAndTppStateTrue(String hashedFiscalCode) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("hashedFiscalCode").is(hashedFiscalCode)),
                Aggregation.unwind("consents"),
                Aggregation.match(Criteria.where("consents.tppState").is(true))
        );

        return mongoTemplate.aggregate(aggregation, CitizenConsent.class, CitizenConsent.class)
                .flatMap(result -> {
                    Map<String, ConsentDetails> validConsents = result.getConsents()
                            .entrySet()
                            .stream()
                            .filter(entry -> entry.getValue().getTppState())
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                    return Flux.just(CitizenConsent.builder()
                            .id(result.getId())
                            .hashedFiscalCode(result.getHashedFiscalCode())
                            .consents(validConsents)
                            .build());
                });
    }

    public Mono<CitizenConsent> findByHashedFiscalCodeAndTppId(String hashedFiscalCode, String tppId) {
        if (tppId == null) {
            return Mono.empty();
        }

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("hashedFiscalCode").is(hashedFiscalCode)),
                Aggregation.match(Criteria.where("consents." + tppId).exists(true))
        );

        return mongoTemplate.aggregate(aggregation, CitizenConsent.class, CitizenConsent.class)
                .next();
    }

}
