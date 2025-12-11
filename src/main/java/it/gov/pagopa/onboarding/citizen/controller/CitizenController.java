package it.gov.pagopa.onboarding.citizen.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 */
@Tag(name = "Citizen Management", description = "API per la gestione dei consensi dei cittadini verso i TPP")
@RequestMapping("/emd/citizen")
public interface CitizenController {

    @Operation(summary = "Upsert Consenso Cittadino", description = "Crea un nuovo consenso o aggiorna quello esistente per la coppia Codice Fiscale - TPP.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Consenso creato o aggiornato con successo",
            content = @Content(schema = @Schema(implementation = CitizenConsentDTO.class))),
        @ApiResponse(responseCode = "400", description = "Formato Codice Fiscale o TPP ID non valido"),
        @ApiResponse(responseCode = "500", description = "Errore interno del server")
    })
    @PostMapping("/{fiscalCode}/{tppId}")
    Mono<ResponseEntity<CitizenConsentDTO>> saveCitizenConsent(
        @Parameter(description = "Codice Fiscale del cittadino", example = "RSSMRA85T10A562S")
        @PathVariable @Pattern(regexp = FISCAL_CODE_STRUCTURE_REGEX, message = "Invalid fiscal code format") String fiscalCode,
        
        @Parameter(description = "Identificativo del TPP", example = "TPP_12345")
        @PathVariable @Pattern(regexp = TPP_STRUCTURE_REGEX, message = "Invalid tpp format") String tppId);


    @Operation(summary = "Cambia Stato Consenso", description = "Abilita o disabilita il consenso per uno specifico TPP.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Stato aggiornato con successo"),
        @ApiResponse(responseCode = "400", description = "Input non valido"),
        @ApiResponse(responseCode = "404", description = "Consenso non trovato")
    })
    @PutMapping("/{fiscalCode}/{tppId}")
    Mono<ResponseEntity<CitizenConsentDTO>> stateSwitch(
        @Parameter(description = "Codice Fiscale del cittadino", example = "RSSMRA85T10A562S")
        @PathVariable @Pattern(regexp = FISCAL_CODE_STRUCTURE_REGEX, message = "Invalid fiscal code format") String fiscalCode,
        
        @Parameter(description = "Identificativo del TPP", example = "TPP_12345")
        @PathVariable @Pattern(regexp = TPP_STRUCTURE_REGEX, message = "Invalid tpp format") String tppId);


    @Operation(summary = "Lista TPP Abilitati", description = "Restituisce la lista degli ID dei TPP per cui il cittadino ha un consenso attivo.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista recuperata (può essere vuota)",
            content = @Content(schema = @Schema(implementation = String.class)))
    })
    @GetMapping("/list/{fiscalCode}/enabled/tpp")
    Mono<ResponseEntity<List<String>>> getTppEnabledList(
        @Parameter(description = "Codice Fiscale del cittadino", example = "RSSMRA85T10A562S")
        @PathVariable @Pattern(regexp = FISCAL_CODE_STRUCTURE_REGEX, message = "Invalid fiscal code format") String fiscalCode);


    @Operation(summary = "Stato Consenso Puntuale", description = "Recupera il dettaglio del consenso per una specifica coppia Cittadino-TPP.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Dettaglio consenso recuperato"),
        @ApiResponse(responseCode = "404", description = "Consenso non trovato")
    })
    @GetMapping("/{fiscalCode}/{tppId}")
    Mono<ResponseEntity<CitizenConsentDTO>> getCitizenConsentStatus(
        @Parameter(description = "Codice Fiscale del cittadino", example = "RSSMRA85T10A562S")
        @PathVariable @Pattern(regexp = FISCAL_CODE_STRUCTURE_REGEX, message = "Invalid fiscal code format") String fiscalCode,
        
        @Parameter(description = "Identificativo del TPP", example = "TPP_12345")
        @PathVariable @Pattern(regexp = TPP_STRUCTURE_REGEX, message = "Invalid tpp format") String tppId);


    @Operation(summary = "Tutti i Consensi del Cittadino", description = "Restituisce l'aggregato completo di tutti i consensi (abilitati e disabilitati) del cittadino.")
    @GetMapping("/list/{fiscalCode}")
    Mono<ResponseEntity<CitizenConsentDTO>> getCitizenConsentsList(
        @Parameter(description = "Codice Fiscale del cittadino", example = "RSSMRA85T10A562S")
        @PathVariable @Pattern(regexp = FISCAL_CODE_STRUCTURE_REGEX, message = "Invalid fiscal code format") String fiscalCode);


    @Operation(summary = "Solo Consensi Abilitati", description = "Restituisce l'aggregato dei soli consensi attivi.")
    @GetMapping("/list/{fiscalCode}/enabled")
    Mono<ResponseEntity<CitizenConsentDTO>> getCitizenConsentsListEnabled(
        @Parameter(description = "Codice Fiscale del cittadino", example = "RSSMRA85T10A562S")
        @PathVariable @Pattern(regexp = FISCAL_CODE_STRUCTURE_REGEX, message = "Invalid fiscal code format") String fiscalCode);


    @Operation(summary = "Cittadini per TPP", description = "Restituisce tutti i consensi attivi associati a uno specifico TPP ID.")
    @GetMapping("/{tppId}")
    Mono<ResponseEntity<List<CitizenConsentDTO>>> getCitizenEnabled(
        @Parameter(description = "Identificativo del TPP", example = "TPP_12345")
        @PathVariable @Pattern(regexp = TPP_STRUCTURE_REGEX, message = "Invalid tpp format") String tppId);


    @Operation(summary = "Cancellazione Totale (TEST)", description = "Elimina l'intero aggregato di consensi per un codice fiscale.⚠️ Endpoint di test/cleanup.")
    @DeleteMapping("/test/delete/{fiscalCode}")
    Mono<ResponseEntity<CitizenConsentDTO>> deleteCitizenConsent(
        @Parameter(description = "Codice Fiscale del cittadino", example = "RSSMRA85T10A562S")
        @PathVariable @Pattern(regexp = FISCAL_CODE_STRUCTURE_REGEX, message = "Invalid fiscal code format") String fiscalCode);


    @Operation(summary = "Verifica Bloom Filter", description = "Verifica rapida se un codice fiscale è presente nel Bloom Filter (ha canali attivi).")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Restituisce 'OK' o 'NO CHANNELS ENABLED'")
    })
    @GetMapping("/filter/{fiscalCode}")
    Mono<ResponseEntity<String>> bloomFilterSearch(
        @Parameter(description = "Codice Fiscale del cittadino", example = "RSSMRA85T10A562S")
        @PathVariable @Pattern(regexp = FISCAL_CODE_STRUCTURE_REGEX, message = "Invalid fiscal code format") String fiscalCode);

}