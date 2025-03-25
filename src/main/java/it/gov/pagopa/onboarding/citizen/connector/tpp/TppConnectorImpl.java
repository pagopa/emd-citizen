package it.gov.pagopa.onboarding.citizen.connector.tpp;

import it.gov.pagopa.onboarding.citizen.dto.TppDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class TppConnectorImpl implements TppConnector {
    private final WebClient webClient;

    public TppConnectorImpl(WebClient.Builder webClientBuilder,
                            @Value("${rest-client.tpp.baseUrl}") String baseUrl) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    @Override
    public Mono<TppDTO> get(String tppId) {
        return webClient.get()
                .uri("/emd/tpp/" + tppId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<>() {
                });
    }
}
