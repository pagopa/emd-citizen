package it.gov.pagopa.onboarding.citizen.controller;

import it.gov.pagopa.onboarding.citizen.dto.CitizenConsentDTO;
import it.gov.pagopa.onboarding.citizen.dto.CitizenConsentStateUpdateDTO;
import it.gov.pagopa.onboarding.citizen.faker.CitizenConsentDTOFaker;
import it.gov.pagopa.onboarding.citizen.faker.CitizenConsentStateUpdateDTOFaker;
import it.gov.pagopa.onboarding.citizen.service.CitizenServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;

@WebFluxTest(CitizenControllerImpl.class)
class CitizenControllerTest {

    @MockBean
    private CitizenServiceImpl citizenService;

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

        Mockito.when(citizenService.updateChannelState(
                        citizenConsentStateUpdateDTO.getHashedFiscalCode(),
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

        Mockito.when(citizenService.getConsentStatus(FISCAL_CODE, TPP_ID))
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
    void getCitizenConsentsEnabled_Ok() {
        List<CitizenConsentDTO> citizenConsentDTOList = List.of(CitizenConsentDTOFaker.mockInstance(true));

        Mockito.when(citizenService.getListEnabledConsents(FISCAL_CODE))
                .thenReturn(Mono.just(citizenConsentDTOList));

        webClient.get()
                .uri("/emd/citizen/list/{fiscalCode}/enabled", FISCAL_CODE)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(CitizenConsentDTO.class)
                .consumeWith(response -> {
                    List<CitizenConsentDTO> resultResponse = response.getResponseBody();
                    Assertions.assertNotNull(resultResponse);
                    Assertions.assertEquals(citizenConsentDTOList.size(), resultResponse.size());
                });
    }

    @Test
    void getCitizenConsents_Ok() {
        List<CitizenConsentDTO> citizenConsentDTOList = List.of(CitizenConsentDTOFaker.mockInstance(true));

        Mockito.when(citizenService.getListAllConsents(FISCAL_CODE))
                .thenReturn(Mono.just(citizenConsentDTOList));

        webClient.get()
                .uri("/emd/citizen/list/{fiscalCode}", FISCAL_CODE)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(CitizenConsentDTO.class)
                .consumeWith(response -> {
                    List<CitizenConsentDTO> resultResponse = response.getResponseBody();
                    Assertions.assertNotNull(resultResponse);
                    Assertions.assertEquals(citizenConsentDTOList.size(), resultResponse.size());
                });
    }
}
