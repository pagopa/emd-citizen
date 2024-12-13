package it.gov.pagopa.onboarding.citizen.controller;

import it.gov.pagopa.onboarding.citizen.dto.CitizenConsentDTO;
import it.gov.pagopa.onboarding.citizen.service.BloomFilterServiceImpl;
import it.gov.pagopa.onboarding.citizen.service.CitizenServiceImpl;
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
    public Mono<ResponseEntity<CitizenConsentDTO>> saveCitizenConsent(String fiscalCode, String tppId) {
        return citizenService.createCitizenConsent(fiscalCode, tppId)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<CitizenConsentDTO>> stateSwitch(String fiscalCode, String tppId) {
        return citizenService.switchState(fiscalCode, tppId)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<CitizenConsentDTO>> getCitizenConsentStatus(String fiscalCode, String tppId) {
        return citizenService.getCitizenConsentStatus(fiscalCode, tppId)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<List<String>>> getTppEnabledList(String fiscalCode) {
        return citizenService.getTppEnabledList(fiscalCode)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<CitizenConsentDTO>> getCitizenConsentsList(String fiscalCode) {
        return citizenService.getCitizenConsentsList(fiscalCode)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<CitizenConsentDTO>> getCitizenConsentsListEnabled(String fiscalCode) {
        return citizenService.getCitizenConsentsListEnabled(fiscalCode)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<List<CitizenConsentDTO>>> getCitizenEnabled(String tppId) {
        return citizenService.getCitizenEnabled(tppId)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<String>> bloomFilterSearch(String fiscalCode) {
        return Mono.fromCallable(() ->
                bloomFilterService.mightContain(fiscalCode) ?
                        ResponseEntity.ok("OK") :
                        ResponseEntity.status(HttpStatus.ACCEPTED).body("NO CHANNELS ENABLED")
        );

    }

}
