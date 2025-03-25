package it.gov.pagopa.onboarding.citizen.connector.tpp;

import it.gov.pagopa.onboarding.citizen.dto.TppDTO;
import reactor.core.publisher.Mono;

public interface TppConnector {
    Mono<TppDTO> get(String tppId);

}