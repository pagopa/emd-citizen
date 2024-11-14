package it.gov.pagopa.onboarding.citizen.controller;

import it.gov.pagopa.onboarding.citizen.dto.CitizenConsentDTO;
import it.gov.pagopa.onboarding.citizen.dto.CitizenConsentStateUpdateDTO;
import it.gov.pagopa.onboarding.citizen.service.BloomFilterServiceImpl;
import it.gov.pagopa.onboarding.citizen.service.CitizenServiceImpl;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
public class CitizenControllerImpl implements CitizenController {

    private final BloomFilterServiceImpl bloomFilterService;

    private final CitizenServiceImpl citizenService;

    public CitizenControllerImpl(BloomFilterServiceImpl bloomFilterService, CitizenServiceImpl citizenService) {
        this.bloomFilterService = bloomFilterService;
        this.citizenService = citizenService;
    }

    @Override
    public Mono<ResponseEntity<CitizenConsentDTO>> saveCitizenConsent(@Valid CitizenConsentDTO citizenConsentDTO) {
        return citizenService.createCitizenConsent(citizenConsentDTO)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<CitizenConsentDTO>> stateUpdate(@Valid CitizenConsentStateUpdateDTO citizenConsentStateUpdateDTO) {
        return citizenService.updateTppState(
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

    @Override
    public Mono<ResponseEntity<String>> getAllFiscalCode(String fiscalCode) {
        return Mono.fromCallable(() ->
                bloomFilterService.mightContain(fiscalCode) ?
                        ResponseEntity.ok("OK") :
                        ResponseEntity.status(HttpStatus.ACCEPTED).body("NO CHANNELS ENABLED")
        );

    }

}
