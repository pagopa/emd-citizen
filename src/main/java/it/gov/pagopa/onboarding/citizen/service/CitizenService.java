package it.gov.pagopa.onboarding.citizen.service;

import it.gov.pagopa.onboarding.citizen.dto.CitizenConsentDTO;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * <p>Service contract for citizen consents management.</p>
 */
public interface CitizenService {

    /**
     * <p>Creates or reuses the consent for the specified TPP.</p>
     *
     * @param fiscalCode plain fiscal code
     * @param tppId TPP identifier
     * @return {@code Mono<CitizenConsentDTO>} DTO containing only the requested consent
     * @throws RuntimeException if TPP is not found
     */
    Mono<CitizenConsentDTO> createCitizenConsent(String fiscalCode, String tppId);

    /**
     * <p>Toggles the consent state for the specified TPP.</p>
     *
     * @param fiscalCode plain fiscal code
     * @param tppId TPP identifier
     * @return {@code Mono<CitizenConsentDTO>} updated consent DTO (only that TPP)
     * @throws RuntimeException if citizen or consent is missing
     */
    Mono<CitizenConsentDTO> switchState(String fiscalCode, String tppId);

    /**
     * <p>Retrieves the consent status for the given fiscal code and TPP id.</p>
     *
     * @param fiscalCode plain fiscal code
     * @param tppId TPP identifier
     * @return {@code Mono<CitizenConsentDTO>} consent DTO
     * @throws RuntimeException if consent is missing
     */
    Mono<CitizenConsentDTO> getCitizenConsentStatus(String fiscalCode, String tppId);

    /**
     * <p>Returns the list of enabled TPP identifiers for the citizen.</p>
     *
     * @param fiscalCode plain fiscal code
     * @return {@code Mono<List<String>>} list of enabled TPP ids (possibly empty)
     */
    Mono<List<String>> getTppEnabledList(String fiscalCode);

    /**
     * <p>Retrieves all consents for the citizen.</p>
     *
     * @param fiscalCode plain fiscal code
     * @return {@code Mono<CitizenConsentDTO>} full consents DTO
     * @throws RuntimeException if citizen is missing
     */
    Mono<CitizenConsentDTO> getCitizenConsentsList(String fiscalCode);

    /**
     * <p>Retrieves only enabled consents for the citizen.</p>
     *
     * @param fiscalCode plain fiscal code
     * @return {@code Mono<CitizenConsentDTO>} filtered DTO (enabled only)
     * @throws RuntimeException if citizen is missing
     */
    Mono<CitizenConsentDTO> getCitizenConsentsListEnabled(String fiscalCode);

    /**
     * <p>Retrieves citizens having an enabled consent for the given TPP id.</p>
     *
     * @param tppId TPP identifier
     * @return {@code Mono<List<CitizenConsentDTO>>} list of citizens (possibly empty)
     */
    Mono<List<CitizenConsentDTO>> getCitizenEnabled(String tppId);

    /**
     * <p>Deletes the citizen consent aggregate.</p>
     *
     * @param fiscalCode plain fiscal code
     * @return {@code Mono<CitizenConsentDTO>} deleted snapshot DTO
     * @throws RuntimeException if citizen is missing
     */
    Mono<CitizenConsentDTO> deleteCitizenConsent(String fiscalCode);

    /**
     * <p>Checks Bloom filter membership and existence of at least one enabled consent.</p>
     *
     * @param fiscalCode plain fiscal code
     * @return {@code Mono<Boolean>} {@code true} if present and an enabled consent exists
     */
    Mono<Boolean> getCitizenInBloomFilter(String fiscalCode);
}
