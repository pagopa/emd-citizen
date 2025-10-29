package it.gov.pagopa.onboarding.citizen.connector.tpp;

import it.gov.pagopa.onboarding.citizen.dto.TppDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * <p>Reactive {@link TppConnector} implementation using Spring {@link WebClient}
 * to fetch Third Party Provider (TPP) data from a remote service.</p>
 */
@Service
public class TppConnectorImpl implements TppConnector {

    /**
     * <p>Configured WebClient pointing to the TPP remote service base URL.</p>
     */
    private final WebClient webClient;

    /**
     * <p>Constructs the connector initializing the WebClient with the provided base URL.</p>
     *
     * @param webClientBuilder Spring-injected builder to create WebClient instances
     * @param baseUrl remote TPP service base URL (property: {@code rest-client.tpp.baseUrl})
     */
    public TppConnectorImpl(WebClient.Builder webClientBuilder,
        @Value("${rest-client.tpp.baseUrl}") String baseUrl) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Performs a reactive HTTP GET to {@code /emd/tpp/{tppId}}.</p>
     */
    @Override
    public Mono<TppDTO> get(String tppId) {
        return webClient.get()
            .uri("/emd/tpp/" + tppId)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<>() {
            });
    }
}
