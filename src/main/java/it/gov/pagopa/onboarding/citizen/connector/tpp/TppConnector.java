package it.gov.pagopa.onboarding.citizen.connector.tpp;

import it.gov.pagopa.onboarding.citizen.dto.TppDTO;
import it.gov.pagopa.onboarding.citizen.dto.TppIdList;
import reactor.core.publisher.Mono;

import java.util.List;

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

    /**
     * <p>Retrieves enabled TPPs from a list of TPP identifiers.</p>
     * <p>Delegates to the emd-tpp service filtering endpoint.</p>
     *
     * @param tppIdList the list of TPP IDs to filter
     * @return {@code Mono<List<TppDTO>>} list of enabled TPPs matching the provided IDs
     */
    Mono<List<TppDTO>> getTppsEnabled(TppIdList tppIdList);
}
