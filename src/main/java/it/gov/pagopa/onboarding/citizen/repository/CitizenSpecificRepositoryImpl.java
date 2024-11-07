package it.gov.pagopa.onboarding.citizen.repository;

import it.gov.pagopa.onboarding.citizen.model.CitizenConsent;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Repository
public class CitizenSpecificRepositoryImpl implements CitizenSpecificRepository {

    private final ReactiveMongoTemplate mongoTemplate;

    public CitizenSpecificRepositoryImpl(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public Mono<List<String>> findByFiscalCodeAndTppStateTrue(String fiscalCode) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("fiscalCode").is(fiscalCode)),
                Aggregation.unwind("consents"),
                Aggregation.match(Criteria.where("consents.tppState").is(true))
        );

        return mongoTemplate.aggregate(aggregation, CitizenConsent.class, CitizenConsent.class)
                .flatMap(result -> {

                    List<String> validConsentIds = result.getConsents()
                            .entrySet()
                            .stream()
                            .filter(entry -> entry.getValue().getTppState())
                            .map(Map.Entry::getKey)
                            .toList();

                    return Mono.just(validConsentIds);
                })
                .collectList()
                .map(lists -> lists.stream()
                        .flatMap(List::stream)
                        .toList());
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
