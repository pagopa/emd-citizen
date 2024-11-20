package it.gov.pagopa.onboarding.citizen.controller;

import it.gov.pagopa.onboarding.citizen.dto.CitizenConsentDTO;
import it.gov.pagopa.onboarding.citizen.dto.CitizenConsentStateUpdateDTO;
import it.gov.pagopa.onboarding.citizen.faker.CitizenConsentDTOFaker;
import it.gov.pagopa.onboarding.citizen.faker.CitizenConsentStateUpdateDTOFaker;
import it.gov.pagopa.onboarding.citizen.service.BloomFilterServiceImpl;
import it.gov.pagopa.onboarding.citizen.service.CitizenServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;

@WebFluxTest(CitizenControllerImpl.class)
class CitizenControllerTest {

    @MockBean
    private CitizenServiceImpl citizenService;

    @MockBean
    private BloomFilterServiceImpl bloomFilterService;

    @Autowired
    private WebTestClient webClient;



    private static final String FISCAL_CODE = "MLXHZZ43A70H203T";
    private static final String TPP_ID  = "tppId";


    @Test
    void saveCitizenConsent_Ok() {
        CitizenConsentDTO citizenConsentDTO = CitizenConsentDTOFaker.mockInstance(true);

        Mockito.when(citizenService.createCitizenConsent(citizenConsentDTO)).thenReturn(Mono.just(citizenConsentDTO));

        webClient.post()
                .uri("/emd/citizen")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(citizenConsentDTO)
                .exchange()
                .expectStatus().isOk()
                .expectBody(CitizenConsentDTO.class)
                .consumeWith(response -> {
                    CitizenConsentDTO resultResponse = response.getResponseBody();
                    Assertions.assertNotNull(resultResponse);
                    Assertions.assertEquals(citizenConsentDTO, resultResponse);
                });
    }

    @Test
    void stateUpdate_Ok() {
        CitizenConsentStateUpdateDTO citizenConsentStateUpdateDTO = CitizenConsentStateUpdateDTOFaker.mockInstance(true);

        CitizenConsentDTO expectedResponseDTO = CitizenConsentDTOFaker.mockInstance(true);

        Mockito.when(citizenService.updateTppState(
                        citizenConsentStateUpdateDTO.getFiscalCode(),
                        citizenConsentStateUpdateDTO.getTppId(),
                        citizenConsentStateUpdateDTO.getTppState()))
                .thenReturn(Mono.just(expectedResponseDTO));

        webClient.put()
                .uri("/emd/citizen/stateUpdate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(citizenConsentStateUpdateDTO)
                .exchange()
                .expectStatus().isOk()
                .expectBody(CitizenConsentDTO.class)
                .consumeWith(response -> {
                    CitizenConsentDTO resultResponse = response.getResponseBody();
                    Assertions.assertNotNull(resultResponse);
                    Assertions.assertEquals(expectedResponseDTO, resultResponse);
                });
    }

    @Test
    void getConsentStatus_Ok() {

        CitizenConsentDTO citizenConsentDTO = CitizenConsentDTOFaker.mockInstance(true);

        Mockito.when(citizenService.getCitizenConsentStatus(FISCAL_CODE, TPP_ID))
                .thenReturn(Mono.just(citizenConsentDTO));

        webClient.get()
                .uri("/emd/citizen/{fiscalCode}/{tppId}", FISCAL_CODE, TPP_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBody(CitizenConsentDTO.class)
                .consumeWith(response -> {
                    CitizenConsentDTO resultResponse = response.getResponseBody();
                    Assertions.assertNotNull(resultResponse);
                    Assertions.assertEquals(citizenConsentDTO, resultResponse);
                });
    }

    @Test
    void getTppEnabledList_Ok() {
        List<String> tppEnabledList = List.of("TPP1", "TPP2");

        Mockito.when(citizenService.getTppEnabledList(FISCAL_CODE))
                .thenReturn(Mono.just(tppEnabledList));

        webClient.get()
                .uri("/emd/citizen/list/{fiscalCode}/enabled", FISCAL_CODE)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<String>>() {})
                .consumeWith(response -> {
                    List<String> resultResponse = response.getResponseBody();
                    Assertions.assertNotNull(resultResponse);
                    Assertions.assertEquals(tppEnabledList.size(), resultResponse.size());
                });
    }

    @Test
    void get_Ok() {
        CitizenConsentDTO citizenConsentDTO = CitizenConsentDTOFaker.mockInstance(true);

        Mockito.when(citizenService.getCitizenConsentsList(FISCAL_CODE))
                .thenReturn(Mono.just(citizenConsentDTO));

        webClient.get()
                .uri("/emd/citizen/list/{fiscalCode}", FISCAL_CODE)
                .exchange()
                .expectStatus().isOk()
                .expectBody(CitizenConsentDTO.class)
                .consumeWith(response -> {
                    CitizenConsentDTO resultResponse = response.getResponseBody();
                    Assertions.assertNotNull(resultResponse);
                    Assertions.assertEquals(citizenConsentDTO, resultResponse);
                });
    }

    @Test
    void getAllFiscalCode_Ok() {
        Mockito.when(bloomFilterService.mightContain(FISCAL_CODE))
                .thenReturn(true);

        webClient.get()
                .uri("/emd/citizen/filter/{fiscalCode}", FISCAL_CODE)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(response -> {
                    String resultResponse = response.getResponseBody();
                    Assertions.assertNotNull(resultResponse);
                    Assertions.assertEquals("OK", resultResponse);
                });
    }
    @Test
    void getAllFiscalCode_NoChannelsEnabled() {
        Mockito.when(bloomFilterService.mightContain(FISCAL_CODE))
                .thenReturn(false);

        webClient.get()
                .uri("/emd/citizen/filter/{fiscalCode}", FISCAL_CODE)
                .exchange()
                .expectStatus().isAccepted()
                .expectBody(String.class)
                .consumeWith(response -> {
                    String resultResponse = response.getResponseBody();
                    Assertions.assertNotNull(resultResponse);
                    Assertions.assertEquals("NO CHANNELS ENABLED", resultResponse);
                });
    }
}
