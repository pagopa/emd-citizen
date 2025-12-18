package it.gov.pagopa.onboarding.citizen.connector.tpp;

import it.gov.pagopa.onboarding.citizen.dto.TppDTO;
import it.gov.pagopa.onboarding.citizen.dto.TppIdList;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

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

    @Test
    void testGetTppsEnabledOk() {

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("[{\"tppId\":\"TPP_OK_1\",\"entityId\":\"ENTITY_OK_1\",\"businessName\":\"Test Business 1\",\"messageUrl\":\"https://example.com/message1\",\"authenticationUrl\":\"https://example.com/auth1\",\"authenticationType\":\"OAUTH2\",\"contact\":{\"name\":\"John Doe\",\"number\":\"+1234567890\",\"email\":\"contact1@example.com\"},\"state\":true},{\"tppId\":\"TPP_OK_2\",\"entityId\":\"ENTITY_OK_2\",\"businessName\":\"Test Business 2\",\"messageUrl\":\"https://example.com/message2\",\"authenticationUrl\":\"https://example.com/auth2\",\"authenticationType\":\"OAUTH2\",\"contact\":{\"name\":\"Jane Doe\",\"number\":\"+0987654321\",\"email\":\"contact2@example.com\"},\"state\":true}]")
                .addHeader("Content-Type", "application/json"));

        TppIdList tppIdList = new TppIdList(List.of("TPP_OK_1", "TPP_OK_2"));
        Mono<List<TppDTO>> resultMono = tppConnector.getTppsEnabled(tppIdList);
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
}
