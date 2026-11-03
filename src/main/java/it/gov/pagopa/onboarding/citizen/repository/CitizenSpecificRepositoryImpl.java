package it.gov.pagopa.onboarding.citizen.repository;

import it.gov.pagopa.onboarding.citizen.model.CitizenConsent;
import it.gov.pagopa.onboarding.citizen.dto.FiscalCodeSearchResult;
import lombok.Data;
import org.bson.Document;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * <p>Implementation of custom MongoDB aggregation queries for {@link CitizenConsent}.</p>
 *
 * <p>This repository handles complex queries that require dynamic field projection
 * and nested document filtering within the {@code consents} map.</p>
 *
 * <p>All methods use MongoDB Aggregation Framework to optimize query performance
 * by projecting only required fields and filtering at database level.</p>
 *
 * @see CitizenSpecificRepository
 */
@Repository
public class CitizenSpecificRepositoryImpl implements CitizenSpecificRepository {

    private final ReactiveMongoTemplate mongoTemplate;

    private static final String FISCAL_CODE = "fiscalCode";

    @Override
    public Mono<Long> countByFiscalCodePrefixOrExact(String normalizedFiscalCode) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(fiscalCodeCriteria(normalizedFiscalCode)),
                Aggregation.count().as("total")
        );
        return mongoTemplate.aggregate(aggregation, "citizen_consents", Document.class)
                .next()
                .map(result -> ((Number) result.get("total")).longValue())
                .defaultIfEmpty(0L);
    }

    @Override
    public Flux<FiscalCodeSearchResult> searchByFiscalCodePrefixOrExact(String normalizedFiscalCode, String afterFiscalCode, int limit) {
        Criteria matchCriteria = fiscalCodeCriteria(normalizedFiscalCode);
        if (afterFiscalCode != null) {
            matchCriteria = new Criteria().andOperator(
                    matchCriteria,
                    Criteria.where(FISCAL_CODE).gt(afterFiscalCode)
            );
        }
        AggregationOperation consentCountProjection = context -> new Document("$project",
                new Document(FISCAL_CODE, 1)
                        .append("consentCount", new Document("$size",
                                new Document("$objectToArray",
                                        new Document("$ifNull", List.of("$consents", new Document()))))));
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(matchCriteria),
                Aggregation.sort(Sort.by(Sort.Direction.ASC, FISCAL_CODE)),
                Aggregation.limit(limit),
                consentCountProjection
        );

        return mongoTemplate.aggregate(aggregation, "citizen_consents", FiscalCodeSearchResult.class);
    }

    private Criteria fiscalCodeCriteria(String normalizedFiscalCode) {
        return normalizedFiscalCode.length() == 16
                ? Criteria.where(FISCAL_CODE).is(normalizedFiscalCode)
                : Criteria.where(FISCAL_CODE).regex("^" + normalizedFiscalCode);
    }

    public CitizenSpecificRepositoryImpl(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * {@inheritDoc}
     *
     * <p><b>MongoDB Aggregation Pipeline:</b></p>
     * <pre>
     * [
     *   { "$match": { "fiscalCode": "&lt;fiscalCode&gt;" } },
     *   { "$project": { "fiscalCode": 1, "consentsArray": { "$objectToArray": "$consents" } } },
     *   { "$match": { "consentsArray.v.tppState": true } }
     * ]
     * </pre>
     *
     * <p><b>Stages explanation:</b></p>
     * <ol>
     *   <li><b>Stage 1 ($match):</b> Filter by fiscal code</li>
     *   <li><b>Stage 2 ($project):</b> Convert {@code consents} map to array for filtering</li>
     *   <li><b>Stage 3 ($match):</b> Filter consents where {@code tppState = true}</li>
     * </ol>
     *
     * <p><b>Note:</b> The result will have {@code consents = null} because the projection
     * transforms the original map structure into {@code consentsArray}.</p>
     *
     * @param fiscalCode citizen's fiscal code
     * @return {@code Mono<CitizenConsent>} with {@code fiscalCode} only (consents is null), empty if no enabled consents
     */
    public Mono<CitizenConsent> findByFiscalCodeWithAtLeastOneConsent(String fiscalCode) {
        AggregationOperation consentsArrayProjection = context -> new Document("$project",
                new Document(FISCAL_CODE, 1)
                        .append("consentsArray", new Document("$objectToArray", "$consents")));
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where(FISCAL_CODE).is(fiscalCode)),
                consentsArrayProjection,
                Aggregation.match(Criteria.where("consentsArray.v.tppState").is(true))
        );

        return mongoTemplate.aggregate(aggregation, "citizen_consents", CitizenConsent.class)
                .next();
    }

    /**
     * {@inheritDoc}
     *
     * <p><b>MongoDB Aggregation Pipeline:</b></p>
     * <pre>
     * [
     *   { "$match": { "fiscalCode": "&lt;fiscalCode&gt;" } },
     *   { "$match": { "consents.&lt;tppId&gt;": { "$exists": true } } },
     *   { "$project": { "fiscalCode": 1, "consents.&lt;tppId&gt;": 1 } }
     * ]
     * </pre>
     *
     * <p><b>Stages explanation:</b></p>
     * <ol>
     *   <li><b>Stage 1 ($match):</b> Filter by fiscal code</li>
     *   <li><b>Stage 2 ($match):</b> Check if {@code consents.<tppId>} exists</li>
     *   <li><b>Stage 3 ($project):</b> Return only {@code fiscalCode} and the specific consent</li>
     * </ol>
     *
     * <p><b>Example result:</b></p>
     * <pre>
     * {
     *   "fiscalCode": "TESTCF00A00B000C",
     *   "consents": {
     *     "e441825b-...": { "tppState": true, "tcDate": "2025-10-29T12:04:21.251" }
     *   }
     * }
     * </pre>
     *
     * @param fiscalCode citizen's fiscal code
     * @param tppId TPP identifier (if {@code null}, returns empty {@code Mono})
     * @return {@code Mono<CitizenConsent>} with single consent, empty if not found or tppId is {@code null}
     */
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

    /**
     * {@inheritDoc}
     *
     * <p><b>MongoDB Aggregation Pipeline:</b></p>
     * <pre>
     * [
     *   { "$match": { "consents.&lt;tppId&gt;.tppState": true } },
     *   { "$project": { "fiscalCode": 1, "consents.&lt;tppId&gt;": 1 } }
     * ]
     * </pre>
     *
     * <p><b>Stages explanation:</b></p>
     * <ol>
     *   <li><b>Stage 1 ($match):</b> Filter documents where {@code consents.<tppId>.tppState = true}</li>
     *   <li><b>Stage 2 ($project):</b> Return only {@code fiscalCode} and the matching consent</li>
     * </ol>
     *
     * <p><b>Performance note:</b> This query can return multiple documents (one per citizen
     * with enabled consent for the given TPP). Consider adding pagination for production use.</p>
     *
     * <p><b>Example result:</b></p>
     * <pre>
     * [
     *   {
     *     "fiscalCode": "TESTCF00A00B000C",
     *     "consents": {
     *       "e441825b-...": { "tppState": true, "tcDate": "2025-10-29T12:04:21.369" }
     *     }
     *   },
     *   { ... }
     * ]
     * </pre>
     *
     * @param tppId TPP identifier
     * @return {@code Flux<CitizenConsent>} emitting all citizens with enabled consent (possibly empty)
     */
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
