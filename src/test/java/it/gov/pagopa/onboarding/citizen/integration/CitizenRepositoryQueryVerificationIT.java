package it.gov.pagopa.onboarding.citizen.integration;

import it.gov.pagopa.onboarding.citizen.model.CitizenConsent;
import it.gov.pagopa.onboarding.citizen.model.ConsentDetails;
import it.gov.pagopa.onboarding.citizen.repository.CitizenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Integration test verifying MongoDB aggregation queries.
 *
 * <p>Uses MongoDB driver debug logging to inspect generated pipelines.
 * Check console output for actual query structure.</p>
 */
@TestPropertySource(properties = {
    "logging.level.org.springframework.data.mongodb.core.ReactiveMongoTemplate=DEBUG",
})
public class CitizenRepositoryQueryVerificationIT extends BaseIT {
    private static final Logger log = LoggerFactory.getLogger(CitizenRepositoryQueryVerificationIT.class);

    private static final String TPP_TEST = "e441825b-ddf2-4067-9b00-33a74aa1bba0-1744118452678";
    private static final String TPP_DISABLED = "disabled-tpp-id";
    private static final String TEST_CF = "TESTCF00A00B000C";
    private static final String TEST_CF_2 = "TESTCF00A00B000D";
    private static final String COLLECTION_NAME = "citizen_consents";

    @Autowired
    ReactiveMongoTemplate mongoTemplate;

    @Autowired
    CitizenRepository repository;

    @BeforeEach
    void setup() {
        // Drop collection
        StepVerifier.create(
            mongoTemplate.dropCollection(COLLECTION_NAME)
                .onErrorResume(e -> Mono.empty())
        ).verifyComplete();

        // Insert test data - Citizen with enabled consent
        CitizenConsent testConsent1 = CitizenConsent.builder()
            .fiscalCode(TEST_CF)
            .consents(Map.of(
                TPP_TEST, ConsentDetails.builder()
                    .tppState(true)
                    .tcDate(LocalDateTime.now())
                    .build(),
                TPP_DISABLED, ConsentDetails.builder()
                    .tppState(false)
                    .tcDate(LocalDateTime.now())
                    .build()
            ))
            .build();

        // Insert test data - Citizen with no enabled consents
        CitizenConsent testConsent2 = CitizenConsent.builder()
            .fiscalCode(TEST_CF_2)
            .consents(Map.of(
                TPP_DISABLED, ConsentDetails.builder()
                    .tppState(false)
                    .tcDate(LocalDateTime.now())
                    .build()
            ))
            .build();

        StepVerifier.create(
            mongoTemplate.save(testConsent1, COLLECTION_NAME)
                .then(mongoTemplate.save(testConsent2, COLLECTION_NAME))
        ).expectNextCount(1).verifyComplete();
    }

    @Test
    void testFindByFiscalCode() {
        log.info("=== EXECUTING findByFiscalCode ===");

        StepVerifier.create(
                repository.findByFiscalCode(TEST_CF)
            )
            .assertNext(consent -> {
                log.info("Found consent: {}", consent);
                assert consent.getFiscalCode().equals(TEST_CF);
                assert consent.getConsents().size() == 2;
            })
            .verifyComplete();

        log.info("=== TEST COMPLETED - CHECK LOGS ABOVE FOR QUERY DETAILS ===");
    }

    @Test
    void testFindByFiscalCodeNotFound() {
        log.info("=== EXECUTING findByFiscalCode (not found) ===");

        StepVerifier.create(
                repository.findByFiscalCode("NOTFOUND")
            )
            .verifyComplete();

        log.info("=== TEST COMPLETED ===");
    }

    @Test
    void testFindByFiscalCodeWithAtLeastOneConsent() {
        log.info("=== EXECUTING findByFiscalCodeWithAtLeastOneConsent ===");

        StepVerifier.create(
                repository.findByFiscalCodeWithAtLeastOneConsent(TEST_CF)
            )
            .assertNext(consent -> {
                log.info("Found consent: {}", consent);
                assert consent.getFiscalCode().equals(TEST_CF);
            })
            .verifyComplete();

        log.info("=== TEST COMPLETED - CHECK LOGS ABOVE FOR QUERY DETAILS ===");
    }

    @Test
    void testFindByFiscalCodeWithAtLeastOneConsentNotFound() {
        log.info("=== EXECUTING findByFiscalCodeWithAtLeastOneConsent (no enabled consents) ===");

        StepVerifier.create(
                repository.findByFiscalCodeWithAtLeastOneConsent(TEST_CF_2)
            )
            .verifyComplete();

        log.info("=== TEST COMPLETED ===");
    }

    @Test
    void testFindByFiscalCodeAndTppId() {
        log.info("=== EXECUTING findByFiscalCodeAndTppId ===");

        StepVerifier.create(
                repository.findByFiscalCodeAndTppId(TEST_CF, TPP_TEST)
            )
            .assertNext(consent -> {
                log.info("Found consent: {}", consent);
                assert consent.getFiscalCode().equals(TEST_CF);
                assert consent.getConsents().containsKey(TPP_TEST);
            })
            .verifyComplete();

        log.info("=== TEST COMPLETED - CHECK LOGS ABOVE FOR QUERY DETAILS ===");
    }

    @Test
    void testFindByFiscalCodeAndTppIdNotFound() {
        log.info("=== EXECUTING findByFiscalCodeAndTppId (tpp not found) ===");

        StepVerifier.create(
                repository.findByFiscalCodeAndTppId(TEST_CF, "non-existent-tpp")
            )
            .verifyComplete();

        log.info("=== TEST COMPLETED ===");
    }

    @Test
    void testFindByFiscalCodeAndTppIdNullTppId() {
        log.info("=== EXECUTING findByFiscalCodeAndTppId (null tppId) ===");

        StepVerifier.create(
                repository.findByFiscalCodeAndTppId(TEST_CF, null)
            )
            .verifyComplete();

        log.info("=== TEST COMPLETED ===");
    }

    @Test
    void testFindByTppIdEnabled() {
        log.info("=== EXECUTING findByTppIdEnabled ===");

        StepVerifier.create(
                repository.findByTppIdEnabled(TPP_TEST)
            )
            .assertNext(consent -> {
                log.info("Found consent: {}", consent);
                assert consent.getFiscalCode().equals(TEST_CF);
                assert consent.getConsents().containsKey(TPP_TEST);
                assert consent.getConsents().get(TPP_TEST).getTppState();
            })
            .verifyComplete();

        log.info("=== TEST COMPLETED - CHECK LOGS ABOVE FOR QUERY DETAILS ===");
    }

    @Test
    void testFindByTppIdEnabledNoResults() {
        log.info("=== EXECUTING findByTppIdEnabled (disabled TPP) ===");

        StepVerifier.create(
                repository.findByTppIdEnabled(TPP_DISABLED)
            )
            .verifyComplete();

        log.info("=== TEST COMPLETED ===");
    }

    @Test
    void testFindByTppIdEnabledMultipleResults() {
        log.info("=== EXECUTING findByTppIdEnabled (multiple results) ===");

        // Add another citizen with same TPP enabled
        CitizenConsent testConsent3 = CitizenConsent.builder()
            .fiscalCode("TESTCF00A00B000E")
            .consents(Map.of(
                TPP_TEST, ConsentDetails.builder()
                    .tppState(true)
                    .tcDate(LocalDateTime.now())
                    .build()
            ))
            .build();

        StepVerifier.create(
            mongoTemplate.save(testConsent3, COLLECTION_NAME)
                .then(Mono.empty())
        ).verifyComplete();

        StepVerifier.create(
                repository.findByTppIdEnabled(TPP_TEST)
            )
            .expectNextCount(2)
            .verifyComplete();

        log.info("=== TEST COMPLETED - CHECK LOGS ABOVE FOR QUERY DETAILS ===");
    }
}
