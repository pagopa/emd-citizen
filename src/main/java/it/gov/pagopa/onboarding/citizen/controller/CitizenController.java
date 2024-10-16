package it.gov.pagopa.onboarding.citizen.controller;

import it.gov.pagopa.onboarding.citizen.dto.CitizenConsentDTO;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;


@RequestMapping("/emd/citizen")
public interface CitizenController {

    @PostMapping("")
    Mono<ResponseEntity<CitizenConsentDTO>> saveCitizenConsent(@Valid @RequestBody CitizenConsentDTO citizenConsentDTO);

    /**
     * Update the state of a citizen's consent.
     *
     * @param citizenConsentDTO contains the hashedFiscalCode, channelId, and channelState to update
     * @return the updated citizen consents
     */
    @PutMapping("/stateUpdate")
    Mono<ResponseEntity<CitizenConsentDTO>> stateUpdate(@Valid @RequestBody CitizenConsentDTO citizenConsentDTO);

    /**
     * Get the consent status for a specific citizen and channel.
     *
     * @param fiscalCode the fiscal code of the citizen
     * @param tppId the ID of the tpp
     * @return the citizen consent status
     */
    @GetMapping("/{fiscalCode}/{tppId}")
    Mono<ResponseEntity<CitizenConsentDTO>> getConsentStatus(@PathVariable String fiscalCode, @PathVariable String tppId);

    /**
     * List all channels with enabled consents for a specific citizen.
     *
     * @param fiscalCode the fiscal code of the citizen
     * @return a list of channels with enabled consents
     */
    @GetMapping("/list/{fiscalCode}/enabled")
    Mono<ResponseEntity<List<CitizenConsentDTO>>> getCitizenConsentsEnabled(@PathVariable String fiscalCode);

    /**
     * List all channels and their consent status for a specific citizen.
     *
     * @param fiscalCode the fiscal code of the citizen
     * @return a list of all channels with their consent statuses
     */
    @GetMapping("/list/{fiscalCode}")
    Mono<ResponseEntity<List<CitizenConsentDTO>>> getCitizenConsents(@PathVariable String fiscalCode);

}
