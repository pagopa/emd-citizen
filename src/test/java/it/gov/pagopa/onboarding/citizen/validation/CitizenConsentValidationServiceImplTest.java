package it.gov.pagopa.onboarding.citizen.validation;

import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import it.gov.pagopa.onboarding.citizen.connector.tpp.TppConnectorImpl;
import it.gov.pagopa.onboarding.citizen.dto.CitizenConsentDTO;
import it.gov.pagopa.onboarding.citizen.dto.TppDTO;
import it.gov.pagopa.onboarding.citizen.dto.mapper.CitizenConsentObjectToDTOMapper;
import it.gov.pagopa.onboarding.citizen.faker.CitizenConsentDTOFaker;
import it.gov.pagopa.onboarding.citizen.faker.CitizenConsentFaker;
import it.gov.pagopa.onboarding.citizen.faker.TppDTOFaker;
import it.gov.pagopa.onboarding.citizen.model.CitizenConsent;
import it.gov.pagopa.onboarding.citizen.repository.CitizenRepository;
import it.gov.pagopa.onboarding.citizen.service.BloomFilterServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
class CitizenConsentValidationServiceImplTest {

    @MockBean
    BloomFilterServiceImpl bloomFilterService;

    @MockBean
    CitizenRepository citizenRepository;

    @MockBean
    TppConnectorImpl tppConnector;

    @MockBean
    CitizenConsentObjectToDTOMapper dtoMapper;

    @Autowired
    CitizenConsentValidationServiceImpl validationService;

    private static final CitizenConsent CITIZEN_CONSENT = CitizenConsentFaker.mockInstance(true);
    private static final CitizenConsentDTO CITIZEN_CONSENT_DTO = CitizenConsentDTOFaker.mockInstance(true);
    private static final TppDTO TPP_DTO = TppDTOFaker.mockInstance();

    @Test
    void handleExistingConsent_ConsentAlreadyExists() {
        CitizenConsent existingConsent = CITIZEN_CONSENT;
        String tppId = "existingTppId";
        existingConsent.getConsents().put(tppId, CITIZEN_CONSENT.getConsents().get(tppId));

        when(dtoMapper.map(existingConsent)).thenReturn(CITIZEN_CONSENT_DTO);

        when(citizenRepository.findAll()).thenReturn(Flux.just(CitizenConsentFaker.mockInstance(true)));

        doNothing().when(bloomFilterService).add(anyString());

        CitizenConsentDTO result = validationService.handleExistingConsent(existingConsent, tppId, CITIZEN_CONSENT).block();

        assertNotNull(result);
        assertEquals(CITIZEN_CONSENT_DTO, result);
    }

    @Test
    void handleExistingConsent_NewConsentForTpp() {
        CitizenConsent existingConsent = CITIZEN_CONSENT;
        TppDTO activeTppDTO = TPP_DTO;
        activeTppDTO.setState(true);

        when(tppConnector.get(anyString())).thenReturn(Mono.just(activeTppDTO));
        when(citizenRepository.save(existingConsent)).thenReturn(Mono.just(existingConsent));
        when(dtoMapper.map(existingConsent)).thenReturn(CITIZEN_CONSENT_DTO);

        when(citizenRepository.findAll()).thenReturn(Flux.just(CitizenConsentFaker.mockInstance(true)));

        doNothing().when(bloomFilterService).add(anyString());

        CitizenConsentDTO result = validationService.handleExistingConsent(existingConsent, activeTppDTO.getTppId(), CITIZEN_CONSENT).block();

        assertNotNull(result);
        assertEquals(CITIZEN_CONSENT_DTO, result);
    }

    @Test
    void validateTppAndSaveConsent_TppValidAndActive() {
        String fiscalCode = CITIZEN_CONSENT.getFiscalCode();
        TppDTO activeTppDTO = TPP_DTO;
        activeTppDTO.setState(true);


        when(tppConnector.get(anyString())).thenReturn(Mono.just(activeTppDTO));
        when(citizenRepository.save(CITIZEN_CONSENT)).thenReturn(Mono.just(CITIZEN_CONSENT));
        when(dtoMapper.map(CITIZEN_CONSENT)).thenReturn(CITIZEN_CONSENT_DTO);

        when(citizenRepository.findAll()).thenReturn(Flux.just(CitizenConsentFaker.mockInstance(true)));

        doNothing().when(bloomFilterService).add(anyString());

        CitizenConsentDTO result = validationService.validateTppAndSaveConsent(fiscalCode, activeTppDTO.getTppId(), CITIZEN_CONSENT).block();

        assertNotNull(result);
        assertEquals(CITIZEN_CONSENT_DTO, result);
        verify(bloomFilterService, times(1)).add(fiscalCode);
    }

    @Test
    void validateTppAndSaveConsent_TppInvalid() {
        String fiscalCode = CITIZEN_CONSENT.getFiscalCode();
        String tppId = "inactiveTppId";

        TppDTO inactiveTppDTO = TPP_DTO;
        inactiveTppDTO.setState(false);

        when(tppConnector.get(anyString())).thenReturn(Mono.just(inactiveTppDTO));

        StepVerifier.create(validationService.validateTppAndSaveConsent(fiscalCode, tppId, CITIZEN_CONSENT))
                .expectErrorMatches(throwable -> throwable instanceof ClientExceptionWithBody &&
                        "TPP is not active or is invalid".equals(throwable.getMessage()) &&
                        "TPP_NOT_FOUND".equals(((ClientExceptionWithBody) throwable).getCode()))
                .verify();

        verify(citizenRepository, never()).save(any());
        verify(bloomFilterService, never()).add(fiscalCode);
    }

    @Test
    void validateTppAndSaveConsent_TppNotFound() {
        String fiscalCode = CITIZEN_CONSENT.getFiscalCode();
        String tppId = "inactiveTppId";

        TppDTO inactiveTppDTO = TPP_DTO;
        inactiveTppDTO.setState(false);

        when(tppConnector.get(anyString())).thenReturn(Mono.just(inactiveTppDTO));

        StepVerifier.create(validationService.validateTppAndSaveConsent(fiscalCode, tppId, CITIZEN_CONSENT))
                .expectErrorMatches(throwable -> throwable instanceof ClientExceptionWithBody &&
                        "TPP does not exist or is not active".equals(throwable.getMessage()) &&
                        "TPP_NOT_FOUND".equals(((ClientExceptionWithBody) throwable).getCode()))
                .verify();

        verify(citizenRepository, never()).save(any());
        verify(bloomFilterService, never()).add(fiscalCode);
    }

}
