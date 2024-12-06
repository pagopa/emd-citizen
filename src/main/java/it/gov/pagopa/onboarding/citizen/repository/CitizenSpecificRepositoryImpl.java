package it.gov.pagopa.onboarding.citizen.repository;

import it.gov.pagopa.onboarding.citizen.model.CitizenConsent;
import lombok.Data;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class CitizenSpecificRepositoryImpl implements CitizenSpecificRepository {

    private final ReactiveMongoTemplate mongoTemplate;

    private static final String FISCAL_CODE = "fiscalCode";

    public CitizenSpecificRepositoryImpl(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }


    public Mono<CitizenConsent> findByFiscalCodeAndTppId(String fiscalCode, String tppId) {
        if (tppId == null) {
            return Mono.empty();
        }

        String consent = "consents." + tppId;
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where(FISCAL_CODE).is(fiscalCode)),
                Aggregation.match(Criteria.where(consent).exists(true)),
                Aggregation.project(FISCAL_CODE).and(consent).as(consent)
        );

        return mongoTemplate.aggregate(aggregation, "citizen_consents", CitizenConsent.class)
                .next();
    }

    public Flux<CitizenConsent> findByTppIdEnabled(String tppId) {
        String consent = "consents." + tppId;
        String tppStatePath = consent + ".tppState";

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where(tppStatePath).is(true)),
                Aggregation.project(FISCAL_CODE).and(consent).as(consent)
        );

        return mongoTemplate.aggregate(aggregation, "citizen_consents", CitizenConsent.class);
    }


    @Data
    public static class ConsentKeyWrapper {
        private String k;
    }

}
