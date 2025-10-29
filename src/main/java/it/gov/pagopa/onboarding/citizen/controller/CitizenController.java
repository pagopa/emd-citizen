package it.gov.pagopa.onboarding.citizen.controller;

import it.gov.pagopa.onboarding.citizen.dto.CitizenConsentDTO;
import it.gov.pagopa.onboarding.citizen.service.CitizenService;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

import static it.gov.pagopa.onboarding.citizen.constants.CitizenConstants.ValidationRegex.FISCAL_CODE_STRUCTURE_REGEX;
import static it.gov.pagopa.onboarding.citizen.constants.CitizenConstants.ValidationRegex.TPP_STRUCTURE_REGEX;

/**
 * <p>Reactive REST contract exposing citizen consent operations.</p>
 *
 * <p>Each method sanitizes raw inputs in the implementation and delegates business logic to {@link CitizenService}.</p>
 *
 * <p>Error semantics and domain flows are documented in the service layer; controller focuses on HTTP contract.</p>
 */
@RequestMapping("/emd/citizen")
public interface CitizenController {

    /**
     * <p>Creates or reuses a consent for the specified TPP.</p>
     * <p>Delegates to {@link CitizenService#createCitizenConsent(String, String)}.</p>
     *
     * @param fiscalCode plain fiscal code (regex validated)
     * @param tppId TPP identifier (regex validated)
     * @return {@code Mono<ResponseEntity<CitizenConsentDTO>>} 200 OK with requested consent DTO
     */
    @PostMapping("/{fiscalCode}/{tppId}")
    Mono<ResponseEntity<CitizenConsentDTO>> saveCitizenConsent(
        @PathVariable @Pattern(regexp = FISCAL_CODE_STRUCTURE_REGEX, message = "Invalid fiscal code format") String fiscalCode,
        @PathVariable @Pattern(regexp = TPP_STRUCTURE_REGEX, message = "Invalid tpp format") String tppId);

    /**
     * <p>Toggles the consent state for the specified TPP.</p>
     * <p>Delegates to {@link CitizenService#switchState(String, String)}.</p>
     *
     * @param fiscalCode plain fiscal code (regex validated)
     * @param tppId TPP identifier (regex validated)
     * @return {@code Mono<ResponseEntity<CitizenConsentDTO>>} 200 OK with updated consent DTO
     */
    @PutMapping("/{fiscalCode}/{tppId}")
    Mono<ResponseEntity<CitizenConsentDTO>> stateSwitch(
        @PathVariable @Pattern(regexp = FISCAL_CODE_STRUCTURE_REGEX, message = "Invalid fiscal code format") String fiscalCode,
        @PathVariable @Pattern(regexp = TPP_STRUCTURE_REGEX, message = "Invalid tpp format") String tppId);

    /**
     * <p>Lists enabled TPP identifiers for the citizen.</p>
     * <p>Delegates to {@link CitizenService#getTppEnabledList(String)}.</p>
     *
     * @param fiscalCode plain fiscal code (regex validated)
     * @return {@code Mono<ResponseEntity<List<String>>>} 200 OK with enabled TPP ids (possibly empty)
     */
    @GetMapping("/list/{fiscalCode}/enabled/tpp")
    Mono<ResponseEntity<List<String>>> getTppEnabledList(
        @PathVariable @Pattern(regexp = FISCAL_CODE_STRUCTURE_REGEX, message = "Invalid fiscal code format") String fiscalCode);

    /**
     * <p>Retrieves consent status for the given TPP.</p>
     * <p>Delegates to {@link CitizenService#getCitizenConsentStatus(String, String)}.</p>
     *
     * @param fiscalCode plain fiscal code (regex validated)
     * @param tppId TPP identifier (regex validated)
     * @return {@code Mono<ResponseEntity<CitizenConsentDTO>>} 200 OK with consent DTO
     */
    @GetMapping("/{fiscalCode}/{tppId}")
    Mono<ResponseEntity<CitizenConsentDTO>> getCitizenConsentStatus(
        @PathVariable @Pattern(regexp = FISCAL_CODE_STRUCTURE_REGEX, message = "Invalid fiscal code format") String fiscalCode,
        @PathVariable @Pattern(regexp = TPP_STRUCTURE_REGEX, message = "Invalid tpp format") String tppId);

    /**
     * <p>Retrieves all consents for the citizen.</p>
     * <p>Delegates to {@link CitizenService#getCitizenConsentsList(String)}.</p>
     *
     * @param fiscalCode plain fiscal code (regex validated)
     * @return {@code Mono<ResponseEntity<CitizenConsentDTO>>} 200 OK with full consents DTO
     */
    @GetMapping("/list/{fiscalCode}")
    Mono<ResponseEntity<CitizenConsentDTO>> getCitizenConsentsList(
        @PathVariable @Pattern(regexp = FISCAL_CODE_STRUCTURE_REGEX, message = "Invalid fiscal code format") String fiscalCode);

    /**
     * <p>Retrieves only enabled consents for the citizen.</p>
     * <p>Delegates to {@link CitizenService#getCitizenConsentsListEnabled(String)}.</p>
     *
     * @param fiscalCode plain fiscal code (regex validated)
     * @return {@code Mono<ResponseEntity<CitizenConsentDTO>>} 200 OK with filtered DTO
     */
    @GetMapping("/list/{fiscalCode}/enabled")
    Mono<ResponseEntity<CitizenConsentDTO>> getCitizenConsentsListEnabled(
        @PathVariable @Pattern(regexp = FISCAL_CODE_STRUCTURE_REGEX, message = "Invalid fiscal code format") String fiscalCode);

    /**
     * <p>Retrieves citizens with an enabled consent for a TPP id.</p>
     * <p>Delegates to {@link CitizenService#getCitizenEnabled(String)}.</p>
     *
     * @param tppId TPP identifier (regex validated)
     * @return {@code Mono<ResponseEntity<List<CitizenConsentDTO>>>} 200 OK with list (possibly empty)
     */
    @GetMapping("/{tppId}")
    Mono<ResponseEntity<List<CitizenConsentDTO>>> getCitizenEnabled(
        @PathVariable @Pattern(regexp = TPP_STRUCTURE_REGEX, message = "Invalid tpp format") String tppId);

    /**
     * <p>Deletes the citizen consent aggregate.</p>
     * <p>Delegates to {@link CitizenService#deleteCitizenConsent(String)}.</p>
     *
     * @param fiscalCode plain fiscal code (regex validated)
     * @return {@code Mono<ResponseEntity<CitizenConsentDTO>>} 200 OK with deleted snapshot DTO
     */
    @DeleteMapping("/test/delete/{fiscalCode}")
    Mono<ResponseEntity<CitizenConsentDTO>> deleteCitizenConsent(
        @PathVariable @Pattern(regexp = FISCAL_CODE_STRUCTURE_REGEX, message = "Invalid fiscal code format") String fiscalCode);

    /**
     * <p>Checks Bloom filter membership and enabled consent existence.</p>
     * <p>Delegates to {@link CitizenService#getCitizenInBloomFilter(String)}; maps boolean to textual status.</p>
     *
     * @param fiscalCode plain fiscal code (regex validated)
     * @return {@code Mono<ResponseEntity<String>>} 200 OK with status ("OK" or "NO CHANNELS ENABLED")
     */
    @GetMapping("/filter/{fiscalCode}")
    Mono<ResponseEntity<String>> bloomFilterSearch(
        @PathVariable @Pattern(regexp = FISCAL_CODE_STRUCTURE_REGEX, message = "Invalid fiscal code format") String fiscalCode);

}
