package it.gov.pagopa.onboarding.citizen.repository;

import it.gov.pagopa.onboarding.citizen.model.CitizenConsent;
import lombok.Data;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class CitizenSpecificRepositoryImpl implements CitizenSpecificRepository {

    private final ReactiveMongoTemplate mongoTemplate;

    public CitizenSpecificRepositoryImpl(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }


    public Mono<CitizenConsent> findByFiscalCodeAndTppId(String fiscalCode, String tppId) {
        if (tppId == null) {
            return Mono.empty();
        }

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("fiscalCode").is(fiscalCode)),
                Aggregation.match(Criteria.where("consents." + tppId).exists(true)),
                Aggregation.project("fiscalCode","consents." + tppId)
        );

        return mongoTemplate.aggregate(aggregation, CitizenConsent.class, CitizenConsent.class)
                .next();
    }

    @Data
    public static class ConsentKeyWrapper {
        private String k;
    }

}
