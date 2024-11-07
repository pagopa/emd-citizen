package it.gov.pagopa.onboarding.citizen.repository;

import it.gov.pagopa.onboarding.citizen.model.CitizenConsent;
import lombok.Data;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class CitizenSpecificRepositoryImpl implements CitizenSpecificRepository {

    private final ReactiveMongoTemplate mongoTemplate;

    public CitizenSpecificRepositoryImpl(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public Mono<List<String>> findByFiscalCodeAndTppStateTrue(String fiscalCode) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("fiscalCode").is(fiscalCode)),
                Aggregation.project("consents").asArray("consentsArray"),
                Aggregation.match(Criteria.where("consentsArray.v.tppState").is(true)),
                Aggregation.project("consentsArray.k").and("key")
        );

        return mongoTemplate.aggregate(aggregation, "citizen_consents", ConsentKeyWrapper.class)
                .collectList()
                .map(results -> results.stream()
                        .map(ConsentKeyWrapper::getKey)
                        .collect(Collectors.toList()));
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
    @Data
    public static class ConsentKeyWrapper {
        private String key;
    }

}
