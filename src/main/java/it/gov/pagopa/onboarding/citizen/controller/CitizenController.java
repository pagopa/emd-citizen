package it.gov.pagopa.onboarding.citizen.controller;

import it.gov.pagopa.onboarding.citizen.dto.CitizenConsentDTO;


import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

import static it.gov.pagopa.onboarding.citizen.constants.CitizenConstants.ValidationRegex.FISCAL_CODE_STRUCTURE_REGEX;
import static it.gov.pagopa.onboarding.citizen.constants.CitizenConstants.ValidationRegex.TPP_STRUCTURE_REGEX;


@RequestMapping("/emd/citizen")
public interface CitizenController {

    @PostMapping("/{fiscalCode}/{tppId}")
    Mono<ResponseEntity<CitizenConsentDTO>> saveCitizenConsent(@PathVariable @Pattern(regexp = FISCAL_CODE_STRUCTURE_REGEX, message = "Invalid fiscal code format") String fiscalCode,
                                                               @PathVariable @Pattern(regexp = TPP_STRUCTURE_REGEX, message = "Invalid fiscal code format") String tppId);

    @PutMapping("/{fiscalCode}/{tppId}")
    Mono<ResponseEntity<CitizenConsentDTO>> stateSwitch(@PathVariable @Pattern(regexp = FISCAL_CODE_STRUCTURE_REGEX, message = "Invalid fiscal code format") String fiscalCode,
                                                        @PathVariable @Pattern(regexp = TPP_STRUCTURE_REGEX, message = "Invalid fiscal code format") String tppId);

    @GetMapping("/filter/{fiscalCode}")
    Mono<ResponseEntity<String>> bloomFilterSearch(@PathVariable @Pattern(regexp = FISCAL_CODE_STRUCTURE_REGEX, message = "Invalid fiscal code format") String fiscalCode);
    
    @GetMapping("/list/{fiscalCode}/enabled/tpp")
    Mono<ResponseEntity<List<String>>> getTppEnabledList(@PathVariable @Pattern(regexp = FISCAL_CODE_STRUCTURE_REGEX, message = "Invalid fiscal code format") String fiscalCode);

    /**
     * Get the consent status for a specific citizen and tpp.
     *
     * @param fiscalCode the fiscal code of the citizen
     * @param tppId the ID of the tpp
     * @return the citizen consent status
     */
    @GetMapping("/{fiscalCode}/{tppId}")
    Mono<ResponseEntity<CitizenConsentDTO>> getCitizenConsentStatus(@PathVariable @Pattern(regexp = FISCAL_CODE_STRUCTURE_REGEX, message = "Invalid fiscal code format") String fiscalCode,
                                                                    @PathVariable @Pattern(regexp = TPP_STRUCTURE_REGEX, message = "Invalid fiscal code format") String tppId);
    
    /**
     * Get consents for a specific citizen.
     *
     * @param fiscalCode the fiscal code of the citizen
     * @return a list of all channels with their consent statuses
     */
    @GetMapping("/list/{fiscalCode}")
    Mono<ResponseEntity<CitizenConsentDTO>> getCitizenConsentsList(@PathVariable @Pattern(regexp = FISCAL_CODE_STRUCTURE_REGEX, message = "Invalid fiscal code format") String fiscalCode);


    @GetMapping("/list/{fiscalCode}/enabled")
    Mono<ResponseEntity<CitizenConsentDTO>> getCitizenConsentsListEnabled(@PathVariable @Pattern(regexp = FISCAL_CODE_STRUCTURE_REGEX, message = "Invalid fiscal code format") String fiscalCode);

    @GetMapping("/{tppId}")
    Mono<ResponseEntity<List<CitizenConsentDTO>>> getCitizenEnabled(@PathVariable @Pattern(regexp = TPP_STRUCTURE_REGEX, message = "Invalid fiscal code format") String fiscalCode);

    @DeleteMapping("/test/delete/{fiscalCode}")
    Mono<ResponseEntity<CitizenConsentDTO>> deleteCitizenConsent(@PathVariable @Pattern(regexp = FISCAL_CODE_STRUCTURE_REGEX, message = "Invalid fiscal code format") String fiscalCode);


}
