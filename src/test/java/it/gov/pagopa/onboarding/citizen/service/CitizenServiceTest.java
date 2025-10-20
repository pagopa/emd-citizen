package it.gov.pagopa.onboarding.citizen.service;

import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import it.gov.pagopa.onboarding.citizen.configuration.ExceptionMap;
import it.gov.pagopa.onboarding.citizen.connector.tpp.TppConnectorImpl;
import it.gov.pagopa.onboarding.citizen.dto.CitizenConsentDTO;
import it.gov.pagopa.onboarding.citizen.dto.TppDTO;
import it.gov.pagopa.onboarding.citizen.dto.mapper.CitizenConsentObjectToDTOMapper;
import it.gov.pagopa.onboarding.citizen.faker.CitizenConsentFaker;
import it.gov.pagopa.onboarding.citizen.faker.TppDTOFaker;
import it.gov.pagopa.onboarding.citizen.model.CitizenConsent;
import it.gov.pagopa.onboarding.citizen.model.ConsentDetails;
import it.gov.pagopa.onboarding.citizen.model.mapper.CitizenConsentDTOToObjectMapper;
import it.gov.pagopa.onboarding.citizen.repository.CitizenRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBloomFilterReactive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@ContextConfiguration(classes = {
        CitizenServiceImpl.class,
        BloomFilterServiceImpl.class,
        CitizenConsentObjectToDTOMapper.class,
        CitizenConsentDTOToObjectMapper.class,
        ExceptionMap.class
})
class CitizenServiceTest {
    @Autowired
    CitizenServiceImpl citizenService;

    @MockBean
    BloomFilterServiceImpl bloomFilterService;

    @MockBean
    CitizenRepository citizenRepository;

    @MockBean
    TppConnectorImpl tppConnector;

    @Autowired
    CitizenConsentObjectToDTOMapper dtoMapper;

    private static final String FISCAL_CODE = "fiscalCode";
    private static final String TPP_ID = "tppId";
    private static final String TPP_ID_2 = "tppId2";
    private static final boolean TPP_STATE = true;
    private static final CitizenConsent CITIZEN_CONSENT = CitizenConsentFaker.mockInstance(true);
    private static final CitizenConsent CITIZEN_CONSENT_2 = CitizenConsentFaker.mockInstance(true);
    private static final TppDTO TPP_DTO = TppDTOFaker.mockInstance();

    @Test
    void createCitizenConsent_Ok() {

        CitizenConsentDTO expectedConsentDTO = dtoMapper.map(CITIZEN_CONSENT);
        TppDTO activeTppDTO = TPP_DTO;
        activeTppDTO.setState(true);

        when(tppConnector.get(anyString())).thenReturn(Mono.just(activeTppDTO));
        when(citizenRepository.save(Mockito.any())).thenReturn(Mono.just(CITIZEN_CONSENT));
        when(citizenRepository.findByFiscalCode(anyString())).thenReturn(Mono.empty());

        StepVerifier.create(citizenService.createCitizenConsent(FISCAL_CODE, TPP_ID))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals(expectedConsentDTO, response);
                })
                .verifyComplete();
    }

    @Test
    void createCitizenConsent_AlreadyExists() {

        CitizenConsentDTO expectedConsentDTO = dtoMapper.map(CITIZEN_CONSENT);

        when(tppConnector.get(anyString())).thenReturn(Mono.just(TPP_DTO));
        when(citizenRepository.save(Mockito.any())).thenReturn(Mono.just(CITIZEN_CONSENT));
        when(citizenRepository.findByFiscalCode(anyString())).thenReturn(Mono.just(CITIZEN_CONSENT));

        StepVerifier.create(citizenService.createCitizenConsent(FISCAL_CODE, TPP_ID))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals(expectedConsentDTO, response);
                })
                .verifyComplete();
    }

    @Test
    void createCitizenConsent_AlreadyOnboardedOnAnotherTpp() {

        when(tppConnector.get(anyString())).thenReturn(Mono.just(TPP_DTO));
        when(citizenRepository.save(Mockito.any())).thenReturn(Mono.just(CITIZEN_CONSENT_2));
        when(citizenRepository.findByFiscalCode(anyString())).thenReturn(Mono.just(CITIZEN_CONSENT));


        StepVerifier.create(citizenService.createCitizenConsent(FISCAL_CODE, TPP_ID_2))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertTrue(response.getConsents().containsKey(TPP_ID_2));
                })
                .verifyComplete();

        CITIZEN_CONSENT.getConsents().remove(TPP_ID_2);
    }

    @Test
    void createCitizenConsent_Ko_TppNotFound() {

        when(tppConnector.get(anyString())).thenReturn(Mono.error(new RuntimeException("TPP not found")));

        StepVerifier.create(citizenService.createCitizenConsent(FISCAL_CODE, TPP_ID))
                .expectErrorMatches(throwable -> throwable instanceof ClientExceptionWithBody &&
                        "TPP does not exist or is not active".equals(throwable.getMessage()) &&
                        "TPP_NOT_FOUND".equals(((ClientExceptionWithBody) throwable).getCode()))
                .verify();

    }

    @Test
    void updateChannelState_Ok() {

        when(citizenRepository.findByFiscalCode(FISCAL_CODE))
                .thenReturn(Mono.just(CITIZEN_CONSENT));

        when(citizenRepository.save(any()))
                .thenReturn(Mono.just(CITIZEN_CONSENT));

        StepVerifier.create(citizenService.switchState(FISCAL_CODE, TPP_ID))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertNotEquals(TPP_STATE, response.getConsents().get(TPP_ID).getTppState());
                })
                .verifyComplete();
    }

    @Test
    void updateChannelState_Ko_CitizenNotOnboardedOnTpp() {

        when(citizenRepository.findByFiscalCode(FISCAL_CODE))
                .thenReturn(Mono.just(CITIZEN_CONSENT));

        StepVerifier.create(citizenService.switchState(FISCAL_CODE, TPP_ID_2))
                .expectErrorMatches(throwable -> throwable instanceof ClientExceptionWithBody &&
                        "Citizen consent not founded during update state process".equals(throwable.getMessage()))
                .verify();
    }

    @Test
    void updateChannelState_Ko_CitizenNotOnboarded() {

        when(citizenRepository.findByFiscalCode(FISCAL_CODE))
                .thenReturn(Mono.empty());

        StepVerifier.create(citizenService.switchState(FISCAL_CODE, TPP_ID))
                .expectErrorMatches(throwable -> throwable instanceof ClientExceptionWithBody &&
                        "Citizen consent not founded during update state process".equals(throwable.getMessage()))
                .verify();
    }

    @Test
    void getConsentStatus_Ok() {

        when(citizenRepository.findByFiscalCodeAndTppId(FISCAL_CODE, TPP_ID))
                .thenReturn(Mono.just(CITIZEN_CONSENT));

        StepVerifier.create(citizenService.getCitizenConsentStatus(FISCAL_CODE, TPP_ID))
                .assertNext(Assertions::assertNotNull)
                .verifyComplete();
    }

    @Test
    void getConsentStatus_Ko_CitizenNotOnboarded() {

        when(citizenRepository.findByFiscalCodeAndTppId(FISCAL_CODE, TPP_ID))
                .thenReturn(Mono.empty());

        StepVerifier.create(citizenService.getCitizenConsentStatus(FISCAL_CODE, TPP_ID))
                .expectErrorMatches(throwable -> throwable instanceof ClientExceptionWithBody &&
                        "Citizen consent not founded".equals(throwable.getMessage()))
                .verify();
    }

    @Test
    void testGetTppEnabledList_Success() {

        Map<String, ConsentDetails> consents = new HashMap<>();

        consents.put("Tpp1", ConsentDetails.builder()
                .tppState(true)
                .tcDate(LocalDateTime.now())
                .build());

        consents.put("Tpp2", ConsentDetails.builder()
                .tppState(false)
                .tcDate(LocalDateTime.now())
                .build());

        CitizenConsent citizenConsent = CitizenConsent.builder()
                .fiscalCode(FISCAL_CODE)
                .consents(consents)
                .build();

        when(citizenRepository.findByFiscalCode(FISCAL_CODE)).thenReturn(Mono.just(citizenConsent));

        StepVerifier.create(citizenService.getTppEnabledList(FISCAL_CODE))
                .assertNext(result -> assertEquals(List.of("Tpp1"), result))
                .verifyComplete();
    }

    @Test
    void testGetTppEnabledList_Empty() {
        when(citizenRepository.findByFiscalCode(FISCAL_CODE)).thenReturn(Mono.empty());

        StepVerifier.create(citizenService.getTppEnabledList(FISCAL_CODE))
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    void get_Ok() {

        when(citizenRepository.findByFiscalCode(FISCAL_CODE))
                .thenReturn(Mono.just(CITIZEN_CONSENT));

        StepVerifier.create(citizenService.getCitizenConsentsList(FISCAL_CODE))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals(CITIZEN_CONSENT.getFiscalCode(), response.getFiscalCode());
                })
                .verifyComplete();
    }

    @Test
    void getCitizenConsentsListEnabled_Ok() {
        Map<String, ConsentDetails> consents = new HashMap<>();
        consents.put("Tpp1", ConsentDetails.builder().tppState(true).tcDate(LocalDateTime.now()).build());
        consents.put("Tpp2", ConsentDetails.builder().tppState(false).tcDate(LocalDateTime.now()).build());

        CitizenConsent citizenConsent = CitizenConsent.builder()
                .fiscalCode(FISCAL_CODE)
                .consents(consents)
                .build();

        CitizenConsentDTO expectedDTO = dtoMapper.map(citizenConsent);
        expectedDTO.getConsents().remove("Tpp2"); // Filter out disabled TPP

        when(citizenRepository.findByFiscalCode(FISCAL_CODE)).thenReturn(Mono.just(citizenConsent));

        StepVerifier.create(citizenService.getCitizenConsentsListEnabled(FISCAL_CODE))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals(1, response.getConsents().size());
                    assertTrue(response.getConsents().containsKey("Tpp1"));
                    assertFalse(response.getConsents().containsKey("Tpp2"));
                })
                .verifyComplete();
    }
    @Test
    void getCitizenConsentsListEnabled_Empty() {
        CitizenConsent citizenConsent = CitizenConsent.builder()
                .fiscalCode(FISCAL_CODE)
                .consents(new HashMap<>())
                .build();

        when(citizenRepository.findByFiscalCode(FISCAL_CODE)).thenReturn(Mono.just(citizenConsent));

        StepVerifier.create(citizenService.getCitizenConsentsListEnabled(FISCAL_CODE))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals(0, response.getConsents().size());
                })
                .verifyComplete();
    }


    @Test
    void getCitizenConsentsListEnabled_NotOnboarded() {
        when(citizenRepository.findByFiscalCode(FISCAL_CODE)).thenReturn(Mono.empty());

        StepVerifier.create(citizenService.getCitizenConsentsListEnabled(FISCAL_CODE))
                .expectErrorMatches(throwable -> throwable instanceof ClientExceptionWithBody &&
                        "Citizen consent not founded during get process ".equals(throwable.getMessage()))
                .verify();
    }

    @Test
    void getCitizenEnabled_Ok() {
        CitizenConsent citizenConsent1 = CitizenConsent.builder()
                .fiscalCode("FiscalCode1")
                .consents(Map.of(TPP_ID, ConsentDetails.builder().tppState(true).tcDate(LocalDateTime.now()).build()))
                .build();

        CitizenConsent citizenConsent2 = CitizenConsent.builder()
                .fiscalCode("FiscalCode2")
                .consents(Map.of(TPP_ID, ConsentDetails.builder().tppState(true).tcDate(LocalDateTime.now()).build()))
                .build();

        when(citizenRepository.findByTppIdEnabled(TPP_ID)).thenReturn(Flux.just(citizenConsent1, citizenConsent2));

        StepVerifier.create(citizenService.getCitizenEnabled(TPP_ID))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals(2, response.size());
                    assertEquals("FiscalCode1", response.get(0).getFiscalCode());
                    assertEquals("FiscalCode2", response.get(1).getFiscalCode());
                })
                .verifyComplete();
    }

    @Test
    void getCitizenEnabled_Empty() {
        when(citizenRepository.findByTppIdEnabled(TPP_ID)).thenReturn(Flux.empty());

        StepVerifier.create(citizenService.getCitizenEnabled(TPP_ID))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertTrue(response.isEmpty());
                })
                .verifyComplete();
    }


    @Test
    void deleteCitizenConsent_OK() {
        CitizenConsent citizenConsent = CitizenConsent.builder()
                .id(FISCAL_CODE)
                .fiscalCode(FISCAL_CODE)
                .consents(new HashMap<>())
                .build();

        when(citizenRepository.findByFiscalCode(FISCAL_CODE)).thenReturn(Mono.just(citizenConsent));
        when(citizenRepository.deleteById(FISCAL_CODE)).thenReturn(Mono.empty());
        StepVerifier.create(citizenService.deleteCitizenConsent(FISCAL_CODE))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals(0, response.getConsents().size());
                })
                .verifyComplete();
    }


    @Test
    void deleteCitizenConsent_NotOnboarded() {
        when(citizenRepository.findByFiscalCode(FISCAL_CODE)).thenReturn(Mono.empty());

        StepVerifier.create(citizenService.deleteCitizenConsent(FISCAL_CODE))
                .expectErrorMatches(throwable -> throwable instanceof ClientExceptionWithBody &&
                        "Citizen consent not founded during delete process ".equals(throwable.getMessage()))
                .verify();
    }

    @Test
    void getCitizenInBloomFilter_NotPresentInBloomFilter() {
        when(bloomFilterService.contains(FISCAL_CODE)).thenReturn(Mono.just(false));

        StepVerifier.create(citizenService.getCitizenInBloomFilter(FISCAL_CODE))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void getCitizenInBloomFilter_PresentInBloomFilter_ConsentExists() {
        when(bloomFilterService.contains(FISCAL_CODE)).thenReturn(Mono.just(true));
        when(citizenRepository.findByFiscalCodeWithAtLeastOneConsent(FISCAL_CODE))
                .thenReturn(Mono.just(CITIZEN_CONSENT));

        StepVerifier.create(citizenService.getCitizenInBloomFilter(FISCAL_CODE))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void getCitizenInBloomFilter_PresentInBloomFilter_NoConsent() {
        when(bloomFilterService.contains(FISCAL_CODE)).thenReturn(Mono.just(true));
        when(citizenRepository.findByFiscalCodeWithAtLeastOneConsent(FISCAL_CODE))
                .thenReturn(Mono.empty());

        StepVerifier.create(citizenService.getCitizenInBloomFilter(FISCAL_CODE))
                .expectNext(false)
                .verifyComplete();
    }
}

