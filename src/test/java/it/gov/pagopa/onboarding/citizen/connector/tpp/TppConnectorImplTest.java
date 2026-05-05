package it.gov.pagopa.onboarding.citizen.connector.tpp;

import it.gov.pagopa.onboarding.citizen.dto.TppDTO;
import it.gov.pagopa.onboarding.citizen.dto.TppIdList;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

import static it.gov.pagopa.onboarding.citizen.enums.AuthenticationType.OAUTH2;
import static org.assertj.core.api.Assertions.assertThat;

class TppConnectorImplTest {

    private MockWebServer mockWebServer;
    private TppConnectorImpl tppConnector;

    // ── helpers ──────────────────────────────────────────────────────────────

    private static final String TPP_JSON =
            "{\"tppId\":\"TPP_OK_1\",\"entityId\":\"ENTITY_OK_1\",\"businessName\":\"Test Business\"," +
            "\"messageUrl\":\"https://example.com/message\",\"authenticationUrl\":\"https://example.com/auth\"," +
            "\"authenticationType\":\"OAUTH2\",\"contact\":{\"name\":\"John Doe\",\"number\":\"+1234567890\"," +
            "\"email\":\"contact@example.com\"},\"state\":true}";

    private static MockResponse okTppResponse() {
        return new MockResponse()
                .setResponseCode(200)
                .setBody(TPP_JSON)
                .addHeader("Content-Type", "application/json");
    }

    /** Enqueues a response that forcefully closes the connection (simulates Connection reset). */
    private static MockResponse connectionResetResponse() {
        return new MockResponse().setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AT_START);
    }

    // ── lifecycle ─────────────────────────────────────────────────────────────

    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        tppConnector = new TppConnectorImpl(WebClient.builder(), mockWebServer.url("/").toString());
    }

    @AfterEach
    void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    // ── existing tests ────────────────────────────────────────────────────────

    @Test
    void testGetTppInfoOk() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"tppId\":\"TPP_OK_1\",\"entityId\":\"ENTITY_OK_1\",\"businessName\":\"Test Business\",\"messageUrl\":\"https://example.com/message\",\"authenticationUrl\":\"https://example.com/auth\",\"authenticationType\":\"OAUTH2\",\"contact\":{\"name\":\"John Doe\",\"number\":\"+1234567890\",\"email\":\"contact@example.com\"},\"state\":true}")
                .addHeader("Content-Type", "application/json"));

        Mono<TppDTO> resultMono = tppConnector.get("TPP_OK_1");
        TppDTO tppDTO = resultMono.block();

        assertThat(tppDTO).isNotNull();
        assertThat(tppDTO.getTppId()).isEqualTo("TPP_OK_1");
        assertThat(tppDTO.getEntityId()).isEqualTo("ENTITY_OK_1");
        assertThat(tppDTO.getBusinessName()).isEqualTo("Test Business");
        assertThat(tppDTO.getMessageUrl()).isEqualTo("https://example.com/message");
        assertThat(tppDTO.getAuthenticationUrl()).isEqualTo("https://example.com/auth");
        assertThat(tppDTO.getAuthenticationType()).isEqualTo(OAUTH2);
        assertThat(tppDTO.getContact().getName()).isEqualTo("John Doe");
        assertThat(tppDTO.getContact().getNumber()).isEqualTo("+1234567890");
        assertThat(tppDTO.getContact().getEmail()).isEqualTo("contact@example.com");
        assertThat(tppDTO.getState()).isTrue();
    }

    @Test
    void testGetTppsEnabledOk() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("[{\"tppId\":\"TPP_OK_1\",\"entityId\":\"ENTITY_OK_1\",\"businessName\":\"Test Business 1\",\"messageUrl\":\"https://example.com/message1\",\"authenticationUrl\":\"https://example.com/auth1\",\"authenticationType\":\"OAUTH2\",\"contact\":{\"name\":\"John Doe\",\"number\":\"+1234567890\",\"email\":\"contact1@example.com\"},\"state\":true},{\"tppId\":\"TPP_OK_2\",\"entityId\":\"ENTITY_OK_2\",\"businessName\":\"Test Business 2\",\"messageUrl\":\"https://example.com/message2\",\"authenticationUrl\":\"https://example.com/auth2\",\"authenticationType\":\"OAUTH2\",\"contact\":{\"name\":\"Jane Doe\",\"number\":\"+0987654321\",\"email\":\"contact2@example.com\"},\"state\":true}]")
                .addHeader("Content-Type", "application/json"));

        String recipientId = "RECIPIENT_OK_1";
        TppIdList tppIdList = new TppIdList(List.of("TPP_OK_1", "TPP_OK_2"), recipientId);
        Mono<List<TppDTO>> resultMono = tppConnector.filterEnabledList(tppIdList);
        List<TppDTO> tppList = resultMono.block();

        assertThat(tppList).isNotNull();
        assertThat(tppList).hasSize(2);

        TppDTO firstTpp = tppList.get(0);
        assertThat(firstTpp.getTppId()).isEqualTo("TPP_OK_1");
        assertThat(firstTpp.getEntityId()).isEqualTo("ENTITY_OK_1");
        assertThat(firstTpp.getBusinessName()).isEqualTo("Test Business 1");
        assertThat(firstTpp.getMessageUrl()).isEqualTo("https://example.com/message1");
        assertThat(firstTpp.getAuthenticationUrl()).isEqualTo("https://example.com/auth1");
        assertThat(firstTpp.getAuthenticationType()).isEqualTo(OAUTH2);
        assertThat(firstTpp.getContact().getName()).isEqualTo("John Doe");
        assertThat(firstTpp.getContact().getNumber()).isEqualTo("+1234567890");
        assertThat(firstTpp.getContact().getEmail()).isEqualTo("contact1@example.com");
        assertThat(firstTpp.getState()).isTrue();

        TppDTO secondTpp = tppList.get(1);
        assertThat(secondTpp.getTppId()).isEqualTo("TPP_OK_2");
        assertThat(secondTpp.getEntityId()).isEqualTo("ENTITY_OK_2");
        assertThat(secondTpp.getBusinessName()).isEqualTo("Test Business 2");
        assertThat(secondTpp.getState()).isTrue();
    }

    // ── retry tests ───────────────────────────────────────────────────────────

    /**
     * Simulates 2 stale-connection drops followed by a good response on get().
     * The retry policy allows 2 attempts → the 3rd call (original + 2 retries) must succeed.
     */
    @Test
    void testGet_retriesOnConnectionResetAndSucceeds() {
        // 1st attempt: connection reset
        mockWebServer.enqueue(connectionResetResponse());
        // 2nd attempt (retry 1): connection reset
        mockWebServer.enqueue(connectionResetResponse());
        // 3rd attempt (retry 2): success
        mockWebServer.enqueue(okTppResponse());

        StepVerifier.create(tppConnector.get("TPP_OK_1"))
                .assertNext(dto -> assertThat(dto.getTppId()).isEqualTo("TPP_OK_1"))
                .verifyComplete();
    }

    /**
     * Simulates 3 consecutive stale-connection drops on get().
     * The retry policy allows only 2 retries → the error must be propagated.
     */
    @Test
    void testGet_exhaustsRetriesAndPropagatesError() {
        mockWebServer.enqueue(connectionResetResponse());
        mockWebServer.enqueue(connectionResetResponse());
        mockWebServer.enqueue(connectionResetResponse());

        StepVerifier.create(tppConnector.get("TPP_OK_1"))
                .expectErrorMatches(ex ->
                        ex instanceof WebClientRequestException ||
                        // reactor wraps exhausted retries in a RetryExhaustedException whose cause is the original
                        (ex.getCause() instanceof WebClientRequestException))
                .verify();
    }

    /**
     * Simulates 2 stale-connection drops followed by a good response on getTppsEnabled().
     * Since this is a POST with the conservative retry policy
     * ({@code connectFailureOnly}), the retry MUST NOT happen — the error from
     * the first disconnect is propagated immediately to avoid duplicate side-effects.
     */
    @Test
    void testGetTppsEnabled_doesNotRetryOnConnectionReset() {
        mockWebServer.enqueue(connectionResetResponse());
        // The successful response below would only be consumed if a retry happened.
        // With connectFailureOnly() it must be left untouched.
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("[]")
                .addHeader("Content-Type", "application/json"));

        StepVerifier.create(tppConnector.filterEnabledList(new TppIdList(List.of("TPP_OK_1"), "RECIPIENT_OK_1")))
                .expectErrorMatches(ex ->
                        ex instanceof WebClientRequestException ||
                        (ex.getCause() instanceof WebClientRequestException))
                .verify();
    }

    /**
     * Simulates 3 consecutive stale-connection drops on getTppsEnabled().
     * No retry happens (POST policy), so the error propagates after the very
     * first attempt regardless of how many subsequent drops are queued.
     */
    @Test
    void testGetTppsEnabled_propagatesErrorOnConnectionResetWithoutRetrying() {
        mockWebServer.enqueue(connectionResetResponse());
        mockWebServer.enqueue(connectionResetResponse());
        mockWebServer.enqueue(connectionResetResponse());

        StepVerifier.create(tppConnector.filterEnabledList(new TppIdList(List.of("TPP_OK_1"), "RECIPIENT_OK_1")))
                .expectErrorMatches(ex ->
                        ex instanceof WebClientRequestException ||
                        (ex.getCause() instanceof WebClientRequestException))
                .verify();
    }
}
