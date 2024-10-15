package it.gov.pagopa.onboarding.citizen.controller;

import it.gov.pagopa.onboarding.citizen.dto.CitizenConsentDTO;
import it.gov.pagopa.onboarding.citizen.service.CitizenServiceImpl;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
public class CitizenControllerImpl implements CitizenController {

    private final CitizenServiceImpl citizenService;

    public CitizenControllerImpl(CitizenServiceImpl citizenService) {
        this.citizenService = citizenService;
    }

    @Override
    public Mono<ResponseEntity<CitizenConsentDTO>> saveCitizenConsent(@Valid CitizenConsentDTO citizenConsentDTO) {
        return citizenService.createCitizenConsent(citizenConsentDTO)
                .map(consent -> new ResponseEntity<>(consent, HttpStatus.CREATED));
    }

    @Override
    public Mono<ResponseEntity<CitizenConsentDTO>> stateUpdate(@Valid CitizenConsentDTO citizenConsentDTO) {
        return citizenService.updateChannelState(
                        citizenConsentDTO.getHashedFiscalCode(),
                        citizenConsentDTO.getTppId(),
                        citizenConsentDTO.getTppState())
                .map(consent -> new ResponseEntity<>(consent, HttpStatus.OK));
    }

    @Override
    public Mono<ResponseEntity<CitizenConsentDTO>> getConsentStatus(String fiscalCode, String tppId) {
        return citizenService.getConsentStatus(fiscalCode, tppId)
                .map(consent -> new ResponseEntity<>(consent, HttpStatus.OK));
    }

    @Override
    public Mono<ResponseEntity<List<CitizenConsentDTO>>> getCitizenConsentsEnabled(String fiscalCode) {
        return citizenService.getListEnabledConsents(fiscalCode)
                .collectList()
                .map(consents -> new ResponseEntity<>(consents, HttpStatus.OK));
    }

    @Override
    public Mono<ResponseEntity<List<CitizenConsentDTO>>> getCitizenConsents(String fiscalCode) {
        return citizenService.getListAllConsents(fiscalCode)
                .collectList()
                .map(consents -> new ResponseEntity<>(consents, HttpStatus.OK));
    }
}
