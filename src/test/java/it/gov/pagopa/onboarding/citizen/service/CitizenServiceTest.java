package it.gov.pagopa.onboarding.citizen.service;

import it.gov.pagopa.common.utils.Utils;
import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import it.gov.pagopa.onboarding.citizen.dto.CitizenConsentDTO;
import it.gov.pagopa.onboarding.citizen.dto.mapper.CitizenConsentMapperToDTO;
import it.gov.pagopa.onboarding.citizen.exception.custom.EmdEncryptionException;
import it.gov.pagopa.onboarding.citizen.exception.custom.ExceptionMap;
import it.gov.pagopa.onboarding.citizen.faker.CitizenConsentDTOFaker;
import it.gov.pagopa.onboarding.citizen.model.CitizenConsent;
import it.gov.pagopa.onboarding.citizen.model.mapper.CitizenConsentMapperToObject;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@ContextConfiguration(classes = {
        CitizenServiceImpl.class,
        CitizenConsentMapperToDTO.class,
        CitizenConsentMapperToObject.class,
        ExceptionMap.class
})
class CitizenServiceTest {

    @Autowired
    CitizenServiceImpl citizenService;

    @MockBean
    CitizenRepository citizenRepository;

    @Autowired
    CitizenConsentMapperToObject mapperToObject;
    @Autowired
    ExceptionMap exceptionMap;

    @Test
    void createCitizenConsent_Ok() {

        CitizenConsentDTO citizenConsentDTO = CitizenConsentDTOFaker.mockInstance(true);
        CitizenConsent citizenConsent = mapperToObject.citizenConsentDTOMapper(citizenConsentDTO);

        Mockito.when(citizenRepository.save(Mockito.any()))
                .thenReturn(Mono.just(citizenConsent));

        CitizenConsentDTO response = citizenService.createCitizenConsent(citizenConsentDTO).block();
        assert response != null;
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
        String hashedFiscalCode = "hashedFiscalCode";
        String tppId = "tppId";
        boolean tppState = true;
        CitizenConsent citizenConsent = new CitizenConsent();

        Mockito.when(citizenRepository.findByHashedFiscalCodeAndTppId(hashedFiscalCode, tppId))
                .thenReturn(Mono.just(citizenConsent));
        Mockito.when(citizenRepository.save(Mockito.any()))
                .thenReturn(Mono.just(citizenConsent));

        CitizenConsentDTO response = citizenService.updateChannelState(hashedFiscalCode, tppId, tppState).block();
        assert response != null;
        assertEquals(tppState, response.getTppState());
    }

    @Test
    void updateChannelState_Ko_CitizenNotOnboarded() {
        String hashedFiscalCode = "hashedFiscalCode";
        String tppId = "tppId";

        Mockito.when(citizenRepository.findByHashedFiscalCodeAndTppId(hashedFiscalCode, tppId))
                .thenReturn(Mono.empty());

        Executable executable = () -> citizenService.updateChannelState(hashedFiscalCode, tppId, true).block();
        ClientExceptionWithBody exception = assertThrows(ClientExceptionWithBody.class, executable);

        assertEquals("CITIZEN_NOT_ONBOARDED", exception.getMessage());
    }

    @Test
    void getConsentStatus_Ok() {
        String fiscalCode = "fiscalCode";
        String tppId = "tppId";
        String hashedFiscalCode = Utils.createSHA256(fiscalCode);
        CitizenConsent citizenConsent = new CitizenConsent();

        Mockito.when(citizenRepository.findByHashedFiscalCodeAndTppId(hashedFiscalCode, tppId))
                .thenReturn(Mono.just(citizenConsent));

        CitizenConsentDTO response = citizenService.getConsentStatus(fiscalCode, tppId).block();
        assert response != null;
    }

    @Test
    void getConsentStatus_Ko_CitizenNotOnboarded() {
        String fiscalCode = "fiscalCode";
        String tppId = "tppId";
        String hashedFiscalCode = Utils.createSHA256(fiscalCode);

        Mockito.when(citizenRepository.findByHashedFiscalCodeAndTppId(hashedFiscalCode, tppId))
                .thenReturn(Mono.empty());

        Executable executable = () -> citizenService.getConsentStatus(fiscalCode, tppId).block();
        ClientExceptionWithBody exception = assertThrows(ClientExceptionWithBody.class, executable);

        assertEquals("CITIZEN_NOT_ONBOARDED", exception.getMessage());
    }

    @Test
    void getListEnabledConsents_Ok() {
        String fiscalCode = "fiscalCode";
        String hashedFiscalCode = Utils.createSHA256(fiscalCode);
        CitizenConsent citizenConsent = new CitizenConsent();

        Mockito.when(citizenRepository.findByHashedFiscalCodeAndTppStateTrue(hashedFiscalCode))
                .thenReturn(Flux.just(citizenConsent));

        Flux<CitizenConsentDTO> response = citizenService.getListEnabledConsents(fiscalCode);
        assertEquals(1, response.count().block());
    }

    @Test
    void getListAllConsents_Ok() {
        String fiscalCode = "fiscalCode";
        String hashedFiscalCode = Utils.createSHA256(fiscalCode);
        CitizenConsent citizenConsent = new CitizenConsent();

        Mockito.when(citizenRepository.findByHashedFiscalCode(hashedFiscalCode))
                .thenReturn(Flux.just(citizenConsent));

        Flux<CitizenConsentDTO> response = citizenService.getListAllConsents(fiscalCode);
        assertEquals(1, response.count().block());
    }
}
