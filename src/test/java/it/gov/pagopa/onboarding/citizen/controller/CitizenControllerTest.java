package it.gov.pagopa.onboarding.citizen.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.onboarding.citizen.dto.CitizenConsentDTO;
import it.gov.pagopa.onboarding.citizen.model.mapper.CitizenConsentMapperToObject;
import it.gov.pagopa.onboarding.citizen.service.CitizenServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

@WebFluxTest(CitizenControllerImpl.class)
class CitizenControllerTest {

    @MockBean
    private CitizenServiceImpl citizenService;

    @Autowired
    private WebTestClient webClient;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    CitizenConsentMapperToObject mapperToObject;

    @Test
    void saveCitizenConsent_Ok() {
        CitizenConsentDTO citizenConsentDTO = new CitizenConsentDTO();

        Mockito.when(citizenService.createCitizenConsent(citizenConsentDTO)).thenReturn(Mono.just(citizenConsentDTO));

        webClient.post()
                .uri("/emd/citizen")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(citizenConsentDTO)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(CitizenConsentDTO.class)
                .consumeWith(response -> {
                    CitizenConsentDTO resultResponse = response.getResponseBody();
                    Assertions.assertNotNull(resultResponse);
                    Assertions.assertEquals(citizenConsentDTO, resultResponse);
                });
    }

    @Test
    void stateUpdate_Ok() {
        CitizenConsentDTO citizenConsentDTO = new CitizenConsentDTO();
        citizenConsentDTO.setHashedFiscalCode("hashedFiscalCode");
        citizenConsentDTO.setTppId("tppId");
        citizenConsentDTO.setTppState(true);

        Mockito.when(citizenService.updateChannelState(
                        citizenConsentDTO.getHashedFiscalCode(),
                        citizenConsentDTO.getTppId(),
                        citizenConsentDTO.getTppState()))
                .thenReturn(Mono.just(citizenConsentDTO));

        webClient.put()
                .uri("/emd/citizen/stateUpdate")
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
    void getConsentStatus_Ok() {
        String fiscalCode = "fiscalCode";
        String tppId = "tppId";
        CitizenConsentDTO citizenConsentDTO = new CitizenConsentDTO();

        Mockito.when(citizenService.getConsentStatus(fiscalCode, tppId))
                .thenReturn(Mono.just(citizenConsentDTO));

        webClient.get()
                .uri("/emd/citizen/{fiscalCode}/{tppId}", fiscalCode, tppId)
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
        String fiscalCode = "fiscalCode";
        List<CitizenConsentDTO> citizenConsentDTOList = Collections.singletonList(new CitizenConsentDTO());

        Mockito.when(citizenService.getListEnabledConsents(fiscalCode))
                .thenReturn(Flux.fromIterable(citizenConsentDTOList));

        webClient.get()
                .uri("/emd/citizen/list/{fiscalCode}/enabled", fiscalCode)
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
        String fiscalCode = "fiscalCode";
        List<CitizenConsentDTO> citizenConsentDTOList = Collections.singletonList(new CitizenConsentDTO());

        Mockito.when(citizenService.getListAllConsents(fiscalCode))
                .thenReturn(Flux.fromIterable(citizenConsentDTOList));

        webClient.get()
                .uri("/emd/citizen/list/{fiscalCode}", fiscalCode)
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
