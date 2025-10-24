package it.gov.pagopa.onboarding.citizen.integration;

import it.gov.pagopa.onboarding.citizen.connector.tpp.TppConnectorImpl;
import it.gov.pagopa.onboarding.citizen.dto.TppDTO;
import it.gov.pagopa.onboarding.citizen.faker.TppDTOFaker;
import it.gov.pagopa.onboarding.citizen.repository.CitizenSpecificRepositoryImpl;
import it.gov.pagopa.onboarding.citizen.service.BloomFilterServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Integration test verifying Bloom Filter update flow:
 * 1. Given a fiscal code not yet added, Bloom Filter returns false.
 * 2. After POST consent creation, the fiscal code is inserted.
 * 3. External TPP connector is mocked to isolate test.
 *
 * The test asserts reactive behavior with StepVerifier.
 */
public class CitizenSpecificRepositoryImplIT extends BaseIT {
    private static final Logger log = LoggerFactory.getLogger(CitizenSpecificRepositoryImplIT.class);

    // Deterministic TPP UUID used for test requests
    private static final String TPP_TEST = "e441825b-ddf2-4067-9b00-33a74aa1bba0-1744118452678";

    @Autowired
    ReactiveMongoTemplate mongoTemplate;
    @Autowired
    CitizenSpecificRepositoryImpl repository;
    @Autowired
    BloomFilterServiceImpl bloomFilterService;

    @MockBean
    private TppConnectorImpl tppConnector;

    /**
     * Drops the collection before each test to ensure isolation.
     * onErrorResume prevents failures if the collection does not exist yet.
     */
    @BeforeEach
    void clean() {
        StepVerifier.create(
                mongoTemplate.dropCollection("citizen_consents")
                        .onErrorResume(e -> Mono.empty())
        ).verifyComplete();
    }

    /**
     * Test scenario:
     * Act: verify absent in Bloom Filter, create consent via REST call, verify presence.
     * Assert: contains() transitions from false to true after consent creation.
     */
    @Test
    void testBloomFilterWebClientAddAndContains() {
        // Mock TPP response
        TppDTO tppDTOFaker = TppDTOFaker.mockInstance();
        when(tppConnector.get(anyString())).thenReturn(Mono.just(tppDTOFaker));

        String testCF = "KKKKKK00D00B000Y";

        // Pre-condition: not contained
        StepVerifier.create(bloomFilterService.contains(testCF))
                .expectNext(false)
                .verifyComplete();

        // REST call that triggers consent creation and Bloom Filter insertion
        webTestClient.post()
                .uri("/emd/citizen/{fiscalCode}/{tppId}", testCF, TPP_TEST)
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is2xxSuccessful();

        // Post-condition: now contained
        StepVerifier.create(bloomFilterService.contains(testCF))
                .expectNext(true)
                .verifyComplete();
    }
    /**
     * Test scenario:
     * Act: verify absent in Bloom Filter, directly add fiscal code, verify presence.
     * Assert: contains() transitions from false to true.
     */
    @Test
    void testBloomFilterAddAndContains() {
        String testCF = "HHHHHH00D00B000Y";

        // Pre-condition: not contained
        StepVerifier.create(bloomFilterService.contains(testCF))
                .expectNext(false)
                .verifyComplete();

        StepVerifier.create(bloomFilterService.add(testCF))
                .verifyComplete();

        // Post-condition: now contained
        StepVerifier.create(bloomFilterService.contains(testCF))
                .expectNext(true)
                .verifyComplete();
    }
}
