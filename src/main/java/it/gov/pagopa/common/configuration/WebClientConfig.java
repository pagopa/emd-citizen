package it.gov.pagopa.common.configuration;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Centralized {@link WebClient.Builder} configuration for all HTTP connectors.
 *
 * <p>Addresses "stale connection" issues caused by Azure load balancers silently
 * closing idle TCP connections. The pool is tuned with explicit {@code maxIdleTime}
 * and {@code maxLifeTime} so connections are recycled before the infrastructure
 * layer discards them.
 *
 * <p>Also adds mandatory connect / read / write timeouts that Netty does not
 * set by default, preventing thread hangs in reactive pipelines.
 */
@Configuration
public class WebClientConfig {

    /** Maximum time a connection may sit idle in the pool before being evicted. */
    private static final Duration MAX_IDLE_TIME = Duration.ofSeconds(20);

    /** Maximum total lifetime of a pooled connection regardless of activity. */
    private static final Duration MAX_LIFE_TIME = Duration.ofSeconds(60);

    /**
     * Maximum number of live connections <strong>per remote host</strong>.
     *
     * <p>Reactor Netty maintains one independent connection bucket per (host, port)
     * pair inside the same {@link ConnectionProvider}. This means that each
     * connector gets its own isolated bucket of up to {@value} connections —
     * there is no contention between them. The default Netty value (500) is
     * intentionally left uncapped, which can exhaust file descriptors in
     * high-concurrency scenarios.
     */
    private static final int MAX_CONNECTIONS = 300;

    /**
     * Maximum number of requests that may queue waiting for a connection when the
     * pool for that remote host is full. Excess requests are immediately rejected
     * with a {@code PoolAcquirePendingLimitException}, which is preferable to
     * unbounded queuing that would hide backpressure problems.
     *
     * <p>Set to {@code 2 × MAX_CONNECTIONS} as a reasonable starting point.
     */
    private static final int PENDING_ACQUIRE_MAX_COUNT = MAX_CONNECTIONS * 2;

    /** TCP connect timeout in milliseconds. */
    private static final int CONNECT_TIMEOUT_MS = 5_000;

    /**
     * Read / write timeout in seconds applied via Netty pipeline handlers.
     *
     * <p>Set equal to {@link #RESPONSE_TIMEOUT} so that both the per-event inactivity
     * guard and the end-to-end cap are consistent. Having IO_TIMEOUT > RESPONSE_TIMEOUT
     * would make the Netty handlers dead-letter code.
     */
    private static final int IO_TIMEOUT_SECONDS = 8;

    /**
     * End-to-end response timeout (hard cap for the whole request).
     *
     * <p><strong>Graceful-shutdown budget constraint:</strong> This value must satisfy
     * {@code N × RESPONSE_TIMEOUT + DB_overhead ≤ spring.lifecycle.timeout (20s)},
     * where N is the maximum number of <em>sequential</em> WebClient calls in a single
     * reactive pipeline. For this service N=1 (single call to emd-tpp), so:
     * {@code 1 × 8s + 2s DB = 10s ≤ 20s}.
     */
    private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(8);

    /** Fail-fast cap for waiting on a free connection when the pool is saturated. */
    private static final Duration PENDING_ACQUIRE_TIMEOUT = Duration.ofSeconds(5);

    /** Period of the background eviction task that removes stale connections proactively. */
    private static final Duration EVICT_IN_BACKGROUND = Duration.ofSeconds(30);

    /**
     * Exposes a pre-configured {@link WebClient.Builder} bean.
     *
     * <p>All connectors/services that need a {@link WebClient} must inject this
     * builder and call {@code .baseUrl(url).build()} on it — never create a
     * raw {@code WebClient.builder()} themselves.
     *
     * <p><strong>Scope prototype:</strong> {@link WebClient.Builder} is mutable.
     * Declaring this bean as prototype ensures that each injection point (e.g.
     * {@code TppConnectorImpl}) receives its own independent builder instance,
     * preventing baseUrl/header mutations from one connector leaking into another.
     * The underlying {@link HttpClient} and {@link ConnectionProvider} are
     * singletons and therefore shared — one pool for all connectors is intentional
     * (resource sharing, single metrics endpoint).
     *
     * @param httpClient the shared singleton {@link HttpClient}
     * @return a fresh, independent {@link WebClient.Builder} per injection
     */
    @Bean
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public WebClient.Builder webClientBuilder(HttpClient httpClient) {
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));
    }

    /**
     * Singleton {@link HttpClient} shared by all {@link WebClient} instances.
     *
     * <p>Wraps the shared {@link ConnectionProvider} and applies connect/read/write
     * timeouts. Being singleton means Netty allocates and manages only one real
     * connection pool for the entire application.
     *
     * @param connectionProvider the shared pool provider
     * @return configured {@link HttpClient}
     */
    @Bean
    public HttpClient httpClient(ConnectionProvider connectionProvider) {
        return HttpClient.create(connectionProvider)
                // TCP handshake timeout
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MS)
                // End-to-end response timeout
                .responseTimeout(RESPONSE_TIMEOUT)
                // Read / Write timeouts injected into the Netty pipeline
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(IO_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(IO_TIMEOUT_SECONDS, TimeUnit.SECONDS)));
    }

    /**
     * Singleton {@link ConnectionProvider} — the actual Netty connection pool.
     *
     * <p>A single named pool is shared across all connectors. This is intentional:
     * it conserves file descriptors, simplifies metrics (one pool to monitor) and
     * avoids re-creating the pool every time a new {@link WebClient.Builder} prototype
     * is requested from the context.
     *
     * <p>To isolate pools per back-end service (e.g. different {@code maxConnections}
     * per target), simply declare additional {@link ConnectionProvider} beans with
     * different names and inject them by qualifier into dedicated {@link HttpClient}
     * beans.
     *
     * @return the shared {@link ConnectionProvider}
     */
    @Bean
    public ConnectionProvider connectionProvider() {
        return ConnectionProvider
                .builder("emd-citizen-http-pool")
                // ── Pool sizing ────────────────────────────────────────────────────────
                // maxConnections is PER REMOTE HOST. With N connectors pointing to N
                // different back-ends, each back-end gets its own isolated bucket of
                // MAX_CONNECTIONS connections. Sharing this ConnectionProvider is safe.
                .maxConnections(MAX_CONNECTIONS)
                // Cap the waiting queue to avoid unbounded memory growth under backpressure.
                .pendingAcquireMaxCount(PENDING_ACQUIRE_MAX_COUNT)
                // Fail-fast when the pool is fully saturated.
                .pendingAcquireTimeout(PENDING_ACQUIRE_TIMEOUT)
                // ── Stale-connection flushing ──────────────────────────────────────────
                .maxIdleTime(MAX_IDLE_TIME)
                .maxLifeTime(MAX_LIFE_TIME)
                // Proactively remove idle/expired connections in background.
                .evictInBackground(EVICT_IN_BACKGROUND)
                // Expose Reactor Netty pool metrics via Micrometer.
                .metrics(true)
                .build();
    }
}

