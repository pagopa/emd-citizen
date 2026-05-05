package it.gov.pagopa.onboarding.citizen.connector.tpp;

import it.gov.pagopa.common.configuration.WebClientRetrySpecs;
import it.gov.pagopa.onboarding.citizen.dto.TppDTO;
import it.gov.pagopa.onboarding.citizen.dto.TppIdList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@Slf4j
public class TppConnectorImpl implements TppConnector {

    private final WebClient webClient;

    /**
     * @param webClientBuilder pre-configured builder from {@code WebClientConfig}
     * @param baseUrl remote TPP service base URL (property: {@code rest-client.tpp.baseUrl})
     */
    public TppConnectorImpl(WebClient.Builder webClientBuilder,
        @Value("${rest-client.tpp.baseUrl}") String baseUrl) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Idempotent GET → permissive retry on any transient network error.
     */
    @Override
    public Mono<TppDTO> get(String tppId) {
        return webClient.get()
            .uri("/emd/tpp/{tppId}", tppId)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<TppDTO>() {})
            .retryWhen(WebClientRetrySpecs.transientNetwork())
            .doOnError(ex -> log.error(
                    "[TPP-CONNECTOR] GET /emd/tpp/{{tppId}} failed: {}", ex.getMessage()));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Non-idempotent POST → conservative retry only on TCP connect failures
     * to avoid duplicate side-effects on the upstream service.
     */
    @Override
    public Mono<List<TppDTO>> filterEnabledList(TppIdList tppIdList) {
        return webClient.post()
                .uri("/emd/tpp/list")
                .bodyValue(tppIdList)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<TppDTO>>() {})
                .retryWhen(WebClientRetrySpecs.connectFailureOnly())
                .doOnError(ex -> log.error(
                        "[TPP-CONNECTOR] POST /emd/tpp/list failed: {}", ex.getMessage()));
    }
}
