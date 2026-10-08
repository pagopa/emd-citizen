package it.gov.pagopa.onboarding.citizen.controller;

import it.gov.pagopa.onboarding.citizen.dto.CitizenConsentDTO;
import it.gov.pagopa.onboarding.citizen.dto.FiscalCodeSearchResult;
import it.gov.pagopa.onboarding.citizen.dto.PagedResponse;
import it.gov.pagopa.onboarding.citizen.service.BloomFilterServiceImpl;
import it.gov.pagopa.onboarding.citizen.service.CitizenServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

import static it.gov.pagopa.common.utils.Utils.inputSanitization;

/**
 * Implementation of the reactive REST contract defined by {@link CitizenController}.
 */
@RestController
public class CitizenControllerImpl implements CitizenController {

    private final CitizenServiceImpl citizenService;

    public CitizenControllerImpl(CitizenServiceImpl citizenService) {
        this.citizenService = citizenService;
    }

    @Override
    public Mono<ResponseEntity<PagedResponse<FiscalCodeSearchResult>>> searchByFiscalCode(String fiscalCode, String cursor, int size) {
        return citizenService.searchByFiscalCode(fiscalCode, cursor, size).map(ResponseEntity::ok);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<ResponseEntity<CitizenConsentDTO>> saveCitizenConsent(String fiscalCode, String tppId) {
        return citizenService.createCitizenConsent(inputSanitization(fiscalCode), inputSanitization(tppId))
            .map(ResponseEntity::ok);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<ResponseEntity<CitizenConsentDTO>> stateSwitch(String fiscalCode, String tppId) {
        return citizenService.switchState(inputSanitization(fiscalCode), inputSanitization(tppId))
            .map(ResponseEntity::ok);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<ResponseEntity<CitizenConsentDTO>> getCitizenConsentStatus(String fiscalCode, String tppId) {
        return citizenService.getCitizenConsentStatus(inputSanitization(fiscalCode), inputSanitization(tppId))
            .map(ResponseEntity::ok);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<ResponseEntity<List<String>>> getTppEnabledList(String fiscalCode) {
        return citizenService.getTppEnabledList(inputSanitization(fiscalCode))
            .map(ResponseEntity::ok);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<ResponseEntity<CitizenConsentDTO>> getCitizenConsentsList(String fiscalCode) {
        return citizenService.getCitizenConsentsList(inputSanitization(fiscalCode))
            .map(ResponseEntity::ok);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<ResponseEntity<CitizenConsentDTO>> getCitizenConsentsListEnabled(String fiscalCode) {
        return citizenService.getCitizenConsentsListEnabled(inputSanitization(fiscalCode))
            .map(ResponseEntity::ok);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<ResponseEntity<List<CitizenConsentDTO>>> getCitizenEnabled(String tppId) {
        return citizenService.getCitizenEnabled(inputSanitization(tppId))
            .map(ResponseEntity::ok);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<ResponseEntity<CitizenConsentDTO>> deleteCitizenConsent(String fiscalCode) {
        return citizenService.deleteCitizenConsent(inputSanitization(fiscalCode))
            .map(ResponseEntity::ok);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<ResponseEntity<String>> bloomFilterSearch(String fiscalCode) {
        return citizenService.getCitizenInBloomFilter(inputSanitization(fiscalCode))
            .map(result -> Boolean.TRUE.equals(result) ? "OK" : "NO CHANNELS ENABLED")
            .map(result -> ResponseEntity.ok().body(result));
    }
}
