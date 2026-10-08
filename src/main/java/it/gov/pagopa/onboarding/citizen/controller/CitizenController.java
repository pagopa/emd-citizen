package it.gov.pagopa.onboarding.citizen.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import it.gov.pagopa.onboarding.citizen.dto.CitizenConsentDTO;
import it.gov.pagopa.onboarding.citizen.dto.CitizenConsentSearchRequest;
import it.gov.pagopa.onboarding.citizen.dto.EnrichedCitizenConsentDTO;
import it.gov.pagopa.onboarding.citizen.dto.FiscalCodeSearchResult;
import it.gov.pagopa.onboarding.citizen.dto.PagedResponse;
import it.gov.pagopa.onboarding.citizen.service.CitizenService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * <p>Reactive REST contract exposing citizen consent operations.</p>
 *
 * <p>Each method sanitizes raw inputs in the implementation and delegates business logic to {@link CitizenService}.</p>
 *
 * <p>Error semantics and domain flows are documented in the service layer; controller focuses on HTTP contract.</p>
 */
@Tag(
    name = "Citizen Management", 
    description = "API per la gestione del ciclo di vita dei consensi del cittadino."
)
@RequestMapping("/emd/citizen")
public interface CitizenController {

    @Operation(summary = "Ricerca paginata per prefisso o Codice Fiscale completo",
            description = "Accetta da 3 a 16 caratteri alfanumerici; usa il cursore restituito per la pagina successiva. size è compresa tra 1 e 100.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Risultati e metadati di paginazione"),
            @ApiResponse(responseCode = "400", description = "Parametri di ricerca o paginazione non validi")
    })
    @GetMapping("/search")
    Mono<ResponseEntity<PagedResponse<FiscalCodeSearchResult>>> searchByFiscalCode(
            @Parameter(description = "Codice Fiscale o suo prefisso", example = "RSSMRA")
            @RequestParam("fiscalCode") String fiscalCode,
            @Parameter(description = "CF dell'ultimo risultato della pagina precedente")
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "size", defaultValue = "10") int size);

    @Operation(
            summary = "Recupera i consensi del cittadino arricchiti con i dati TPP",
            description = "Riceve il Codice Fiscale nel body, cerca il documento del cittadino e recupera entityId e businessName per ogni TPP."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Consensi recuperati con i dettagli TPP",
                    content = @Content(schema = @Schema(implementation = EnrichedCitizenConsentDTO.class))),
            @ApiResponse(responseCode = "400", description = "Codice Fiscale mancante o non valido"),
            @ApiResponse(responseCode = "404", description = "Cittadino non trovato"),
            @ApiResponse(responseCode = "500", description = "Errore interno o del connettore TPP")
    })
    @PostMapping("/consent/search")
    Mono<ResponseEntity<EnrichedCitizenConsentDTO>> searchCitizenConsents(
            @Valid @RequestBody CitizenConsentSearchRequest request);

    /**
     * <p>Creates or reuses a consent for the specified TPP.</p>
     * <p>Delegates to {@link CitizenService#createCitizenConsent(String, String)}.</p>
     * <p><b>Endpoint:</b> {@code POST /emd/citizen/{fiscalCode}/{tppId}}</p>
     *
     * @param fiscalCode plain fiscal code (regex validated)
     * @param tppId TPP identifier (regex validated)
     * @return {@code Mono<ResponseEntity<CitizenConsentDTO>>} 200 OK with requested consent DTO
     */
    @Operation(
        summary = "Creates or reuses a consent for the specified TPP",
        description = "Initializes or updates a consent relationship between the Citizen and the TPP."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Consent created or retrieved successfully",
            content = @Content(schema = @Schema(implementation = CitizenConsentDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid Fiscal Code or TPP format")
    })
    @PostMapping("/{fiscalCode}/{tppId}")
    Mono<ResponseEntity<CitizenConsentDTO>> saveCitizenConsent(
        @Parameter(description = "Plain fiscal code", example = "RSSMRA85T10A562S", required = true)
        @PathVariable String fiscalCode,

        @Parameter(description = "TPP identifier", example = "TPP_XYZ_123", required = true)
        @PathVariable String tppId);

    /**
     * <p>Toggles the consent state for the specified TPP.</p>
     * <p>Delegates to {@link CitizenService#switchState(String, String)}.</p>
     * <p><b>Endpoint:</b> {@code PUT /emd/citizen/{fiscalCode}/{tppId}}</p>
     *
     * @param fiscalCode plain fiscal code (regex validated)
     * @param tppId TPP identifier (regex validated)
     * @return {@code Mono<ResponseEntity<CitizenConsentDTO>>} 200 OK with updated consent DTO
     */
    @Operation(
        summary = "Toggles the consent state for the specified TPP",
        description = "Switches the state (ENABLED/DISABLED) of an existing consent."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Consent state updated successfully"),
        @ApiResponse(responseCode = "404", description = "Consent not found")
    })
    @PutMapping("/{fiscalCode}/{tppId}")
    Mono<ResponseEntity<CitizenConsentDTO>> stateSwitch(
        @Parameter(description = "Plain fiscal code", example = "RSSMRA85T10A562S")
        @PathVariable String fiscalCode,

        @Parameter(description = "TPP identifier", example = "TPP_XYZ_123")
        @PathVariable String tppId);

    /**
     * <p>Lists enabled TPP identifiers for the citizen.</p>
     * <p>Delegates to {@link CitizenService#getTppEnabledList(String)}.</p>
     * <p><b>Endpoint:</b> {@code GET /emd/citizen/list/{fiscalCode}/enabled/tpp}</p>
     *
     * @param fiscalCode plain fiscal code (regex validated)
     * @return {@code Mono<ResponseEntity<List<String>>>} 200 OK with enabled TPP ids (possibly empty)
     */
    @Operation(
        summary = "Lists enabled TPP identifiers for the citizen",
        description = "Retrieves a list of TPP IDs for which the citizen has given active consent."
    )
    @GetMapping("/list/{fiscalCode}/enabled/tpp")
    Mono<ResponseEntity<List<String>>> getTppEnabledList(
        @Parameter(description = "Plain fiscal code", example = "RSSMRA85T10A562S")
        @PathVariable String fiscalCode);

    /**
     * <p>Retrieves consent status for the given TPP.</p>
     * <p>Delegates to {@link CitizenService#getCitizenConsentStatus(String, String)}.</p>
     * <p><b>Endpoint:</b> {@code GET /emd/citizen/{fiscalCode}/{tppId}}</p>
     *
     * @param fiscalCode plain fiscal code (regex validated)
     * @param tppId TPP identifier (regex validated)
     * @return {@code Mono<ResponseEntity<CitizenConsentDTO>>} 200 OK with consent DTO
     */
    @Operation(
        summary = "Retrieves consent status for the given TPP",
        description = "Fetches the specific consent details for a citizen and TPP pair."
    )
    @GetMapping("/{fiscalCode}/{tppId}")
    Mono<ResponseEntity<CitizenConsentDTO>> getCitizenConsentStatus(
        @Parameter(description = "Plain fiscal code", example = "RSSMRA85T10A562S")
        @PathVariable String fiscalCode,

        @Parameter(description = "TPP identifier", example = "TPP_XYZ_123")
        @PathVariable String tppId);

    /**
     * <p>Retrieves all consents for the citizen.</p>
     * <p>Delegates to {@link CitizenService#getCitizenConsentsList(String)}.</p>
     * <p><b>Endpoint:</b> {@code GET /emd/citizen/list/{fiscalCode}}</p>
     *
     * @param fiscalCode plain fiscal code (regex validated)
     * @return {@code Mono<ResponseEntity<CitizenConsentDTO>>} 200 OK with full consents DTO
     */
    @Operation(
        summary = "Retrieves all consents for the citizen",
        description = "Returns the aggregate object containing all consents associated with the fiscal code."
    )
    @GetMapping("/list/{fiscalCode}")
    Mono<ResponseEntity<CitizenConsentDTO>> getCitizenConsentsList(
        @Parameter(description = "Plain fiscal code", example = "RSSMRA85T10A562S")
        @PathVariable String fiscalCode);

    /**
     * <p>Retrieves only enabled consents for the citizen.</p>
     * <p>Delegates to {@link CitizenService#getCitizenConsentsListEnabled(String)}.</p>
     * <p><b>Endpoint:</b> {@code GET /emd/citizen/list/{fiscalCode}/enabled}</p>
     *
     * @param fiscalCode plain fiscal code (regex validated)
     * @return {@code Mono<ResponseEntity<CitizenConsentDTO>>} 200 OK with filtered DTO
     */
    @Operation(
        summary = "Retrieves only enabled consents for the citizen",
        description = "Returns the aggregate object containing only the active (enabled) consents."
    )
    @GetMapping("/list/{fiscalCode}/enabled")
    Mono<ResponseEntity<CitizenConsentDTO>> getCitizenConsentsListEnabled(
        @Parameter(description = "Plain fiscal code", example = "RSSMRA85T10A562S")
        @PathVariable String fiscalCode);

    /**
     * <p>Retrieves citizens with an enabled consent for a TPP id.</p>
     * <p>Delegates to {@link CitizenService#getCitizenEnabled(String)}.</p>
     * <p><b>Endpoint:</b> {@code GET /emd/citizen/{tppId}}</p>
     *
     * @param tppId TPP identifier (regex validated)
     * @return {@code Mono<ResponseEntity<List<CitizenConsentDTO>>>} 200 OK with list (possibly empty)
     */
    @Operation(
        summary = "Retrieves citizens with an enabled consent for a TPP id",
        description = "Reverse lookup: finds all citizens who have consented to a specific TPP."
    )
    @GetMapping("/{tppId}")
    Mono<ResponseEntity<List<CitizenConsentDTO>>> getCitizenEnabled(
        @Parameter(description = "TPP identifier", example = "TPP_XYZ_123")
        @PathVariable String tppId);

    /**
     * <p>Deletes the citizen consent aggregate.</p>
     * <p>Delegates to {@link CitizenService#deleteCitizenConsent(String)}.</p>
     * <p><b>Endpoint:</b> {@code DELETE /emd/citizen/test/delete/{fiscalCode}}</p>
     *
     * @param fiscalCode plain fiscal code (regex validated)
     * @return {@code Mono<ResponseEntity<CitizenConsentDTO>>} 200 OK with deleted snapshot DTO
     */
    @Operation(
        summary = "Deletes the citizen consent aggregate (Test/Cleanup)",
        description = "Completely removes the consent aggregate. Warning: intended for testing purposes."
    )
    @DeleteMapping("/test/delete/{fiscalCode}")
    Mono<ResponseEntity<CitizenConsentDTO>> deleteCitizenConsent(
        @Parameter(description = "Plain fiscal code", example = "RSSMRA85T10A562S")
        @PathVariable String fiscalCode);

    /**
     * <p>Checks Bloom filter membership and enabled consent existence.</p>
     * <p>Delegates to {@link CitizenService#getCitizenInBloomFilter(String)}; maps boolean to textual status.</p>
     * <p><b>Endpoint:</b> {@code GET /emd/citizen/filter/{fiscalCode}}</p>
     *
     * @param fiscalCode plain fiscal code (regex validated)
     * @return {@code Mono<ResponseEntity<String>>} 200 OK with status ("OK" or "NO CHANNELS ENABLED")
     */
    @Operation(
        summary = "Checks Bloom filter membership and enabled consent existence",
        description = "Rapid check to verify if a fiscal code has any active channels without full DB lookup."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Returns 'OK' or 'NO CHANNELS ENABLED'")
    })
    @GetMapping("/filter/{fiscalCode}")
    Mono<ResponseEntity<String>> bloomFilterSearch(
        @Parameter(description = "Plain fiscal code", example = "RSSMRA85T10A562S")
        @PathVariable String fiscalCode);

}