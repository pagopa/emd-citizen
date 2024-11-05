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

    public Flux<CitizenConsent> findByFiscalCodeAndTppStateTrue(String fiscalCode) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("fiscalCode").is(fiscalCode)),
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
                            .fiscalCode(result.getFiscalCode())
                            .consents(validConsents)
                            .build());
                });
    }

    public Mono<CitizenConsent> findByFiscalCodeAndTppId(String fiscalCode, String tppId) {
        if (tppId == null) {
            return Mono.empty();
        }

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("fiscalCode").is(fiscalCode)),
                Aggregation.match(Criteria.where("consents." + tppId).exists(true))
        );

        return mongoTemplate.aggregate(aggregation, CitizenConsent.class, CitizenConsent.class)
                .next();
    }

}
