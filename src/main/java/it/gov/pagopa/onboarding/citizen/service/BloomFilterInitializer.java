package it.gov.pagopa.onboarding.citizen.service;

import it.gov.pagopa.onboarding.citizen.model.CitizenConsent;
import it.gov.pagopa.onboarding.citizen.model.ConsentDetails;
import it.gov.pagopa.onboarding.citizen.repository.CitizenRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilterReactive;
import org.redisson.api.RedissonReactiveClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <p>Initializes and manages a Redis-backed Bloom Filter for efficient citizen consent lookups.</p>
 *
 * <p>This component provides:</p>
 * <ul>
 *   <li><b>Startup initialization:</b> Creates and populates Bloom Filter on application start</li>
 *   <li><b>Scheduled reset:</b> Rebuilds the filter daily at 4 AM to prevent false positive rate degradation</li>
 *   <li><b>Distributed locking:</b> Ensures only one instance performs initialization/reset in clustered deployments</li>
 * </ul>
 *
 * <b>Bloom Filter Configuration</b>
 * <p>The filter is configured via application properties:</p>
 * <ul>
 *   <li><code>app.bloomFilter.expectedInsertions</code>: Expected number of fiscal codes (default sizing)</li>
 *   <li><code>app.bloomFilter.falseProbability</code>: Target false positive rate (e.g., 0.01 = 1%)</li>
 * </ul>
 *
 * <b>Workflow</b>
 * <pre>
 * 1. Acquire distributed lock (60s timeout)
 * 2. Check if Bloom Filter exists in Redis
 *    - If exists → populate with fiscal codes
 *    - If not exists → initialize structure, then populate
 * 3. Release lock
 * </pre>
 *
 * <b>Data Source Query</b>
 * <p>Uses {@link CitizenRepository#findAll()} which executes:</p>
 * <pre>
 * db.citizen_consents.find({})
 * </pre>
 * <p>Filters in-memory for citizens with at least one consent where {@code tppState = true}.</p>
 *
 * <b>Performance Considerations</b>
 * <ul>
 *   <li><b>Batch processing:</b> Fiscal codes are added in batches of 100 to reduce Redis round-trips</li>
 *   <li><b>Memory usage:</b> Bloom Filter memory = {@code -n*ln(p) / (ln(2)^2)} where n=expectedInsertions, p=falseProbability</li>
 *   <li><b>Scheduled reset:</b> Prevents filter degradation as new citizens are added over time</li>
 * </ul>
 *
 * @see RBloomFilterReactive
 * @see CitizenRepository
 */
@Component
@Slf4j
public class BloomFilterInitializer {

    private static final String REDIS_BF_NAME = "emd-bloom-filter";
    private static final String REDIS_LOCK_NAME = "startup-task-lock";

    @Getter
    private final RBloomFilterReactive<String> bloomFilter;
    private final RedissonReactiveClient redissonClient;
    private final CitizenRepository citizenRepository;

    @Value("${app.bloomFilter.expectedInsertions}")
    @Getter
    private long expectedInsertions;

    @Value("${app.bloomFilter.falseProbability}")
    @Getter
    private double falseProbability;

    public BloomFilterInitializer(RedissonReactiveClient redissonClient,
        CitizenRepository citizenRepository) {
        this.redissonClient = redissonClient;
        this.citizenRepository = citizenRepository;
        this.bloomFilter = redissonClient.getBloomFilter(REDIS_BF_NAME);
    }

    /**
     * <p>Initializes the Bloom Filter on application startup.</p>
     *
     * <p>Workflow:</p>
     * <ol>
     *   <li>Acquire distributed lock (prevents concurrent initialization in clustered setup)</li>
     *   <li>Check if Bloom Filter already exists in Redis</li>
     *   <li>Initialize structure if needed, then populate with fiscal codes</li>
     *   <li>Release lock</li>
     * </ol>
     *
     * <p><b>Note:</b> If lock cannot be acquired, initialization is skipped (another instance is handling it).</p>
     */
    @PostConstruct
    public void initialize() {
        acquireLock()
            .flatMap(this::processInitialization)
            .subscribe();
    }

    /**
     * <p>Attempts to acquire a distributed lock for Bloom Filter operations.</p>
     *
     * @return {@code Mono<Boolean>} emitting {@code true} if lock acquired, {@code false} otherwise
     */
    private Mono<Boolean> acquireLock() {
        return redissonClient.getLock(REDIS_LOCK_NAME).tryLock(0, 60, TimeUnit.SECONDS);
    }

    /**
     * <p>Processes Bloom Filter initialization based on lock acquisition status.</p>
     *
     * @param lockAcquired {@code true} if distributed lock was acquired
     * @return {@code Mono<Void>} completing when initialization is done or skipped
     */
    private Mono<Void> processInitialization(Boolean lockAcquired) {
        if (Boolean.FALSE.equals(lockAcquired)) {
            return logLockNotAcquired();
        }
        return bloomFilter.isExists()
            .flatMap(exists -> Boolean.TRUE.equals(exists) ? populateBloomFilter()
                : initializeBloomFilter())
            .doFinally(signal -> releaseLock());
    }

    /**
     * <p>Logs a message when lock acquisition fails during initialization.</p>
     *
     * @return {@code Mono<Void>} that logs and completes immediately
     */
    private Mono<Void> logLockNotAcquired() {
        return Mono.fromRunnable(
            () -> log.info("[BLOOM-FILTER-INITIALIZER] Another instance is initializing."));
    }

    /**
     * <p>Initializes Bloom Filter structure in Redis with configured capacity and false positive rate.</p>
     *
     * <p>After initialization, automatically triggers population with fiscal codes.</p>
     *
     * @return {@code Mono<Void>} completing when initialization and population are done
     */
    private Mono<Void> initializeBloomFilter() {
        return bloomFilter.tryInit(expectedInsertions, falseProbability)
            .doOnSuccess(result -> log.info(
                "[BLOOM-FILTER-INITIALIZER] Bloom filter created with {} expected insertions and {} false probability",
                expectedInsertions, falseProbability))
            .onErrorResume(error -> {
                log.error("[BLOOM-FILTER-INITIALIZER] Initialization failed", error);
                return Mono.empty();
            })
            .flatMap(result -> bloomFilter.isExists().flatMap(
                exists -> Boolean.TRUE.equals(exists) ? populateBloomFilter() : Mono.empty()));
    }

    /**
     * <p>Populates Bloom Filter with fiscal codes from all citizens with at least one enabled consent.</p>
     *
     * <p><b>Data source query:</b></p>
     * <pre>
     * db.citizen_consents.find({})
     * </pre>
     *
     * <p><b>Filtering logic:</b></p>
     * <ul>
     *   <li>Only citizens with at least one consent where {@code tppState = true} are added</li>
     *   <li>Fiscal codes are processed in batches of 100 for performance</li>
     * </ul>
     *
     * @return {@code Mono<Void>} completing when all fiscal codes are added
     */
    private Mono<Void> populateBloomFilter() {
        return citizenRepository.findAll()
            .filter(this::hasValidConsent)
            .map(CitizenConsent::getFiscalCode)
            .buffer(100)
            .flatMap(this::addBatchToBloomFilter)
            .then(Mono.fromRunnable(
                () -> log.info("[BLOOM-FILTER-INITIALIZER] Population complete")));
    }

    /**
     * <p>Checks if a citizen has at least one consent with {@code tppState = true}.</p>
     *
     * @param citizen the citizen to check
     * @return {@code true} if at least one consent is enabled, {@code false} otherwise
     */
    private boolean hasValidConsent(CitizenConsent citizen) {
        return citizen.getConsents().values().stream().anyMatch(ConsentDetails::getTppState);
    }

    /**
     * <p>Adds a batch of fiscal codes to the Bloom Filter.</p>
     *
     * @param batch list of fiscal codes to add
     * @return {@code Mono<Void>} completing when all codes are added
     */
    private Mono<Void> addBatchToBloomFilter(List<String> batch) {
        return Flux.fromIterable(batch)
            .flatMap(bloomFilter::add)
            .then();
    }

    /**
     * <p>Scheduled task that resets the Bloom Filter daily at 4 AM.</p>
     *
     * <p><b>Purpose:</b></p>
     * <ul>
     *   <li>Prevents false positive rate degradation as new citizens are added</li>
     *   <li>Ensures filter remains optimized for current dataset size</li>
     * </ul>
     *
     * <p><b>Workflow:</b></p>
     * <ol>
     *   <li>Acquire distributed lock</li>
     *   <li>Delete existing Bloom Filter from Redis</li>
     *   <li>Re-initialize with current configuration</li>
     *   <li>Re-populate with latest fiscal codes</li>
     *   <li>Release lock</li>
     * </ol>
     */
    @Scheduled(cron = "0 0 4 * * ?")
    public void resetBloomFilter() {
        acquireLock()
            .flatMap(this::processReset)
            .subscribe();
    }

    /**
     * <p>Processes Bloom Filter reset based on lock acquisition status.</p>
     *
     * @param lockAcquired {@code true} if distributed lock was acquired
     * @return {@code Mono<Void>} completing when reset is done or skipped
     */
    private Mono<Void> processReset(Boolean lockAcquired) {
        if (Boolean.FALSE.equals(lockAcquired)) {
            return logLockNotAcquiredForReset();
        }
        return performReset()
            .doFinally(signal -> releaseLock());
    }

    /**
     * <p>Logs a message when lock acquisition fails during scheduled reset.</p>
     *
     * @return {@code Mono<Void>} that logs and completes immediately
     */
    private Mono<Void> logLockNotAcquiredForReset() {
        return Mono.fromRunnable(() -> log.info(
            "[BLOOM-FILTER-INITIALIZER] Another instance is resetting the Bloom Filter."));
    }

    /**
     * <p>Performs the actual Bloom Filter reset operation.</p>
     *
     * @return {@code Mono<Void>} completing when reset is done
     */
    private Mono<Void> performReset() {
        log.info("[BLOOM-FILTER-INITIALIZER] Resetting Bloom Filter...");
        return bloomFilter.delete()
            .then(initializeBloomFilter())
            .doOnSuccess(v -> log.info("[BLOOM-FILTER-INITIALIZER] Bloom Filter reset completed."))
            .doOnError(
                error -> log.error("[BLOOM-FILTER-INITIALIZER] Error during Bloom Filter reset",
                    error));
    }

    /**
     * <p>Releases the distributed lock acquired for Bloom Filter operations.</p>
     */
    private void releaseLock() {
        redissonClient.getLock(REDIS_LOCK_NAME).unlock();
        log.info("[BLOOM-FILTER-INITIALIZER] Lock released.");
    }

}