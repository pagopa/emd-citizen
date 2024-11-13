package it.gov.pagopa.onboarding.citizen.service;

import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import it.gov.pagopa.onboarding.citizen.configuration.ExceptionMap;
import it.gov.pagopa.onboarding.citizen.connector.tpp.TppConnectorImpl;
import it.gov.pagopa.onboarding.citizen.dto.CitizenConsentDTO;
import it.gov.pagopa.onboarding.citizen.dto.TppDTO;
import it.gov.pagopa.onboarding.citizen.dto.mapper.CitizenConsentObjectToDTOMapper;
import it.gov.pagopa.onboarding.citizen.faker.CitizenConsentDTOFaker;
import it.gov.pagopa.onboarding.citizen.faker.CitizenConsentFaker;
import it.gov.pagopa.onboarding.citizen.faker.TppDTOFaker;
import it.gov.pagopa.onboarding.citizen.model.CitizenConsent;
import it.gov.pagopa.onboarding.citizen.model.ConsentDetails;
import it.gov.pagopa.onboarding.citizen.model.mapper.CitizenConsentDTOToObjectMapper;
import it.gov.pagopa.onboarding.citizen.repository.CitizenRepository;
import it.gov.pagopa.onboarding.citizen.validation.CitizenConsentValidationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
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
        CitizenConsentValidationServiceImpl.class,
        CitizenConsentObjectToDTOMapper.class,
        CitizenConsentDTOToObjectMapper.class,
        ExceptionMap.class
})
class CitizenServiceTest {

    @Autowired
    CitizenServiceImpl citizenService;

    @MockBean
    CitizenConsentValidationServiceImpl validationService;

    @MockBean
    CitizenRepository citizenRepository;

    @MockBean
    TppConnectorImpl tppConnector;

    @Autowired
    CitizenConsentObjectToDTOMapper dtoMapper;

    private static final String FISCAL_CODE = "fiscalCode";
    private static final String TPP_ID = "tppId";
    private static final boolean TPP_STATE = true;
    private static final CitizenConsent CITIZEN_CONSENT = CitizenConsentFaker.mockInstance(true);
    private static final CitizenConsentDTO CITIZEN_CONSENT_DTO = CitizenConsentDTOFaker.mockInstance(true);
    private static final TppDTO TPP_DTO = TppDTOFaker.mockInstance();

    @Test
    void createCitizenConsent_Ok() {

        CitizenConsentDTO expectedConsentDTO = dtoMapper.map(CITIZEN_CONSENT);
        TppDTO activeTppDTO = TPP_DTO;
        activeTppDTO.setState(true);

        when(tppConnector.get(anyString())).thenReturn(Mono.just(activeTppDTO));
        when(citizenRepository.save(Mockito.any())).thenReturn(Mono.just(CITIZEN_CONSENT));
        when(citizenRepository.findByFiscalCode(anyString())).thenReturn(Mono.empty());
        when(validationService.validateTppAndSaveConsent(anyString(), anyString(), any(CitizenConsent.class)))
                .thenReturn(Mono.just(expectedConsentDTO));

        CitizenConsentDTO response = citizenService.createCitizenConsent(CITIZEN_CONSENT_DTO).block();
        assertNotNull(response);
        assertEquals(expectedConsentDTO, response);
    }

    @Test
    void createCitizenConsent_AlreadyExists() {

        CitizenConsentDTO expectedConsentDTO = dtoMapper.map(CITIZEN_CONSENT);
        TppDTO activeTppDTO = TPP_DTO;
        activeTppDTO.setState(true);

        when(tppConnector.get(anyString())).thenReturn(Mono.just(activeTppDTO));

        when(citizenRepository.save(Mockito.any())).thenReturn(Mono.just(CITIZEN_CONSENT));
        when(citizenRepository.findByFiscalCode(anyString())).thenReturn(Mono.just(CITIZEN_CONSENT));
        when(citizenRepository.findByFiscalCodeAndTppId(anyString(), anyString())).thenReturn(Mono.just(CITIZEN_CONSENT));

        when(validationService.handleExistingConsent(any(), anyString(), any()))
                .thenReturn(Mono.just(expectedConsentDTO));

        when(validationService.validateTppAndSaveConsent(anyString(), anyString(), any(CitizenConsent.class)))
                .thenReturn(Mono.just(expectedConsentDTO));

        CitizenConsentDTO response = citizenService.createCitizenConsent(CITIZEN_CONSENT_DTO).block();

        assertNotNull(response);
        assertEquals(expectedConsentDTO, response);
    }

    @Test
    void createCitizenConsent_Ko_TppNotProvided() {

        CitizenConsentDTO incompleteConsentDTO = CitizenConsentDTOFaker.mockInstance(true);
        incompleteConsentDTO.getConsents().clear();

        when(tppConnector.get(anyString())).thenReturn(Mono.empty());
        when(citizenRepository.findByFiscalCodeAndTppId(anyString(), anyString())).thenReturn(Mono.empty());

        when(validationService.validateTppAndSaveConsent(anyString(), anyString(), any(CitizenConsent.class)))
                .thenReturn(Mono.error(new ClientExceptionWithBody(HttpStatus.BAD_REQUEST, "TPP_NOT_FOUND", "TPP does not exist or is not active")));

        StepVerifier.create(citizenService.createCitizenConsent(incompleteConsentDTO))
                .expectErrorMatches(throwable -> throwable instanceof ClientExceptionWithBody &&
                        "TPP does not exist or is not active".equals(throwable.getMessage()) &&
                        "TPP_NOT_FOUND".equals(((ClientExceptionWithBody) throwable).getCode()))
                .verify();
    }

    @Test
    void createCitizenConsent_Ko_TppInactive() {

        TppDTO inactiveTppDTO = TPP_DTO;
        inactiveTppDTO.setState(false);

        when(tppConnector.get(anyString())).thenReturn(Mono.just(inactiveTppDTO));
        when(citizenRepository.findByFiscalCodeAndTppId(anyString(), anyString())).thenReturn(Mono.empty());
        when(citizenRepository.findByFiscalCode(anyString())).thenReturn(Mono.empty());

        when(validationService.validateTppAndSaveConsent(anyString(), anyString(), any(CitizenConsent.class)))
                .thenReturn(Mono.error(new ClientExceptionWithBody(HttpStatus.BAD_REQUEST, "TPP_NOT_FOUND", "TPP does not exist or is not active")));

        CitizenConsentDTO citizenConsentDTO = dtoMapper.map(CITIZEN_CONSENT);

        StepVerifier.create(citizenService.createCitizenConsent(citizenConsentDTO))
                .expectErrorMatches(throwable -> throwable instanceof ClientExceptionWithBody &&
                        "TPP does not exist or is not active".equals(throwable.getMessage()) &&
                        "TPP_NOT_FOUND".equals(((ClientExceptionWithBody) throwable).getCode()))
                .verify();

        Mockito.verify(citizenRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void updateChannelState_Ok() {

        TppDTO mockTppDTO = TppDTOFaker.mockInstance();
        mockTppDTO.setState(true);

        when(tppConnector.get(anyString()))
                .thenReturn(Mono.just(mockTppDTO));

        when(citizenRepository.findByFiscalCodeAndTppId(FISCAL_CODE, TPP_ID))
                .thenReturn(Mono.just(CITIZEN_CONSENT));

        when(citizenRepository.save(any()))
                .thenReturn(Mono.just(CITIZEN_CONSENT));


        CitizenConsentDTO response = citizenService.updateTppState(FISCAL_CODE, TPP_ID, TPP_STATE).block();

        assertNotNull(response);

        assertEquals(TPP_STATE, response.getConsents().get(TPP_ID).getTppState());
    }

    @Test
    void updateChannelState_Ko_CitizenNotOnboarded() {

        TppDTO mockTppDTO = TppDTOFaker.mockInstance();
        mockTppDTO.setState(true);

        when(tppConnector.get(anyString()))
                .thenReturn(Mono.just(mockTppDTO));

        when(citizenRepository.findByFiscalCodeAndTppId(FISCAL_CODE, TPP_ID))
                .thenReturn(Mono.empty());



        Executable executable = () -> citizenService.updateTppState(FISCAL_CODE, TPP_ID, true).block();
        ClientExceptionWithBody exception = assertThrows(ClientExceptionWithBody.class, executable);

        assertEquals("Citizen consent not founded during update state process", exception.getMessage());
    }

    @Test
    void getConsentStatus_Ok() {


        when(citizenRepository.findByFiscalCodeAndTppId(FISCAL_CODE, TPP_ID))
                .thenReturn(Mono.just(CITIZEN_CONSENT));

        CitizenConsentDTO response = citizenService.getConsentStatus(FISCAL_CODE, TPP_ID).block();
        assertNotNull(response);
    }

    @Test
    void getConsentStatus_Ko_CitizenNotOnboarded() {

        when(citizenRepository.findByFiscalCodeAndTppId(FISCAL_CODE, TPP_ID))
                .thenReturn(Mono.empty());

        Executable executable = () -> citizenService.getConsentStatus(FISCAL_CODE, TPP_ID).block();
        ClientExceptionWithBody exception = assertThrows(ClientExceptionWithBody.class, executable);

        assertEquals("Citizen consent not founded during get process ", exception.getMessage());
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

        Mono<List<String>> result = citizenService.getTppEnabledList(FISCAL_CODE);

        StepVerifier.create(result)
                .expectNext(List.of("Tpp1"))
                .verifyComplete();
    }

    @Test
    void get_Ok() {

        when(citizenRepository.findByFiscalCode(FISCAL_CODE))
                .thenReturn(Mono.just(CITIZEN_CONSENT));

        CitizenConsentDTO response = citizenService.get(FISCAL_CODE).block();

        assertNotNull(response);

        assertEquals(CITIZEN_CONSENT.getFiscalCode(), response.getFiscalCode());
    }

}
