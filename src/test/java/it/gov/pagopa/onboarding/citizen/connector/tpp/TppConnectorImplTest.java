package it.gov.pagopa.onboarding.citizen.connector.tpp;

import it.gov.pagopa.onboarding.citizen.dto.TppDTO;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static it.gov.pagopa.onboarding.citizen.enums.AuthenticationType.OAUTH2;
import static org.assertj.core.api.Assertions.assertThat;

class TppConnectorImplTest {

    private MockWebServer mockWebServer;
    private TppConnectorImpl tppConnector;

    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        WebClient.Builder webClientBuilder = WebClient.builder();

        tppConnector = new TppConnectorImpl(webClientBuilder, mockWebServer.url("/").toString());
    }

    @AfterEach
    void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

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
}
