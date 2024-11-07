package it.gov.pagopa.onboarding.citizen.controller;

import it.gov.pagopa.onboarding.citizen.dto.CitizenConsentDTO;
import it.gov.pagopa.onboarding.citizen.dto.CitizenConsentStateUpdateDTO;
import it.gov.pagopa.onboarding.citizen.service.CitizenServiceImpl;
import jakarta.validation.Valid;
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
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<CitizenConsentDTO>> stateUpdate(@Valid CitizenConsentStateUpdateDTO citizenConsentStateUpdateDTO) {
        return citizenService.updateChannelState(
                        citizenConsentStateUpdateDTO.getFiscalCode(),
                        citizenConsentStateUpdateDTO.getTppId(),
                        citizenConsentStateUpdateDTO.getTppState())
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<CitizenConsentDTO>> getConsentStatus(String fiscalCode, String tppId) {
        return citizenService.getConsentStatus(fiscalCode, tppId)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<List<String>>> getTppEnabledList(String fiscalCode) {
        return citizenService.getTppEnabledList(fiscalCode)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<CitizenConsentDTO>> get(String fiscalCode) {
        return citizenService.get(fiscalCode)
                .map(ResponseEntity::ok);
    }

}
