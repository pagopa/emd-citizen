package it.gov.pagopa.onboarding.citizen.service;

import it.gov.pagopa.common.utils.Utils;
import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import it.gov.pagopa.onboarding.citizen.dto.CitizenConsentDTO;
import it.gov.pagopa.onboarding.citizen.dto.mapper.CitizenConsentObjectToDTOMapper;
import it.gov.pagopa.common.web.exception.EmdEncryptionException;
import it.gov.pagopa.onboarding.citizen.configuration.ExceptionMap;
import it.gov.pagopa.onboarding.citizen.faker.CitizenConsentDTOFaker;
import it.gov.pagopa.onboarding.citizen.faker.CitizenConsentFaker;
import it.gov.pagopa.onboarding.citizen.model.CitizenConsent;
import it.gov.pagopa.onboarding.citizen.model.mapper.CitizenConsentDTOToObjectMapper;
import it.gov.pagopa.onboarding.citizen.repository.CitizenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@ContextConfiguration(classes = {
        CitizenServiceImpl.class,
        CitizenConsentObjectToDTOMapper.class,
        CitizenConsentDTOToObjectMapper.class,
        ExceptionMap.class
})
class CitizenServiceTest {

    @Autowired
    CitizenServiceImpl citizenService;

    @MockBean
    CitizenRepository citizenRepository;

    @Autowired
    CitizenConsentDTOToObjectMapper mapperToObject;

    private static final String FISCAL_CODE = "fiscalCode";
    private static final String HASHED_FISCAL_CODE = Utils.createSHA256(FISCAL_CODE);
    private static final String TPP_ID = "tppId";
    private static final boolean TPP_STATE = true;
    private static final CitizenConsent CITIZEN_CONSENT = CitizenConsentFaker.mockInstance(true);

    @Test
    void createCitizenConsent_Ok() {

        CitizenConsentDTO citizenConsentDTO = CitizenConsentDTOFaker.mockInstance(true);
        CitizenConsent citizenConsent = mapperToObject.map(citizenConsentDTO);

        Mockito.when(citizenRepository.save(Mockito.any()))
                .thenReturn(Mono.just(citizenConsent));

        CitizenConsentDTO response = citizenService.createCitizenConsent(citizenConsentDTO).block();
        assertNotNull(response);
        citizenConsentDTO.setLastUpdateDate(response.getLastUpdateDate());
        citizenConsentDTO.setCreationDate(response.getCreationDate());

        assertEquals(citizenConsentDTO, response);
    }

    @Test
    void createCitizenConsent_Ko_EmdEncryptError() {
        CitizenConsentDTO citizenConsentDTO = CitizenConsentDTOFaker.mockInstance(true);

        try (MockedStatic<Utils> mockedStatic = Mockito.mockStatic(Utils.class)) {
            mockedStatic.when(() -> Utils.createSHA256(any()))
                    .thenThrow(EmdEncryptionException.class);

            assertThrows(EmdEncryptionException.class, () -> citizenService.createCitizenConsent(citizenConsentDTO));
        }
    }

    @Test
    void updateChannelState_Ok() {

        

        Mockito.when(citizenRepository.findByHashedFiscalCodeAndTppId(HASHED_FISCAL_CODE, TPP_ID))
                .thenReturn(Mono.just(CITIZEN_CONSENT));
        Mockito.when(citizenRepository.save(Mockito.any()))
                .thenReturn(Mono.just(CITIZEN_CONSENT));

        CitizenConsentDTO response = citizenService.updateChannelState(FISCAL_CODE, TPP_ID, TPP_STATE).block();
        assertNotNull(response);
        assertEquals(TPP_STATE, response.getTppState());
    }

    @Test
    void updateChannelState_Ko_CitizenNotOnboarded() {


        Mockito.when(citizenRepository.findByHashedFiscalCodeAndTppId(HASHED_FISCAL_CODE, TPP_ID))
                .thenReturn(Mono.empty());

        Executable executable = () -> citizenService.updateChannelState(FISCAL_CODE, TPP_ID, true).block();
        ClientExceptionWithBody exception = assertThrows(ClientExceptionWithBody.class, executable);

        assertEquals("CITIZEN_NOT_ONBOARDED", exception.getMessage());
    }

    @Test
    void getConsentStatus_Ok() {
        

        Mockito.when(citizenRepository.findByHashedFiscalCodeAndTppId(HASHED_FISCAL_CODE, TPP_ID))
                .thenReturn(Mono.just(CITIZEN_CONSENT));

        CitizenConsentDTO response = citizenService.getConsentStatus(FISCAL_CODE, TPP_ID).block();
        assertNotNull(response);
    }

    @Test
    void getConsentStatus_Ko_CitizenNotOnboarded() {

        Mockito.when(citizenRepository.findByHashedFiscalCodeAndTppId(HASHED_FISCAL_CODE, TPP_ID))
                .thenReturn(Mono.empty());

        Executable executable = () -> citizenService.getConsentStatus(FISCAL_CODE, TPP_ID).block();
        ClientExceptionWithBody exception = assertThrows(ClientExceptionWithBody.class, executable);

        assertEquals("CITIZEN_NOT_ONBOARDED", exception.getMessage());
    }

    @Test
    void getListEnabledConsents_Ok() {
        

        Mockito.when(citizenRepository.findByHashedFiscalCodeAndTppStateTrue(HASHED_FISCAL_CODE))
                .thenReturn(Flux.just(CITIZEN_CONSENT));

        List<CitizenConsentDTO> response = citizenService.getListEnabledConsents(FISCAL_CODE).block();
        assertNotNull(response);
        assertEquals(1, response.size());
    }

    @Test
    void getListAllConsents_Ok() {
        

        Mockito.when(citizenRepository.findByHashedFiscalCode(HASHED_FISCAL_CODE))
                .thenReturn(Flux.just(CITIZEN_CONSENT));

        List<CitizenConsentDTO> response = citizenService.getListAllConsents(FISCAL_CODE).block();
        assertNotNull(response);
        assertEquals(1, response.size());
    }
}
