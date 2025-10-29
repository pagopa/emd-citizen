package it.gov.pagopa.onboarding.citizen.connector.tpp;

import it.gov.pagopa.onboarding.citizen.dto.TppDTO;
import reactor.core.publisher.Mono;

/**
 * <p>Abstraction for retrieving Third Party Provider (TPP) data.</p>
 */
public interface TppConnector {

    /**
     * <p>Retrieves the TPP associated with the provided identifier.</p>
     *
     * @param tppId technical or functional identifier of the TPP
     * @return {@code Mono<TppDTO>} emitting the found DTO or {@code Mono.empty()} if missing
     */
    Mono<TppDTO> get(String tppId);
}
