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

    public BloomFilterInitializer(RedissonReactiveClient redissonClient, CitizenRepository citizenRepository) {
        this.redissonClient = redissonClient;
        this.citizenRepository = citizenRepository;
        this.bloomFilter = redissonClient.getBloomFilter(REDIS_BF_NAME);
    }


    @PostConstruct
    public void initialize() {
        acquireLock()
                .flatMap(this::processInitialization)
                .subscribe();
    }

    private Mono<Boolean> acquireLock() {
        return redissonClient.getLock(REDIS_LOCK_NAME).tryLock(0, 60, TimeUnit.SECONDS);
    }

    private Mono<Void> processInitialization(Boolean lockAcquired) {
        if (Boolean.FALSE.equals(lockAcquired)) {
            return logLockNotAcquired();
        }
        return bloomFilter.isExists()
                .flatMap(exists -> Boolean.TRUE.equals(exists) ? populateBloomFilter() : initializeBloomFilter())
                .doFinally(signal -> releaseLock());
    }

    private Mono<Void> logLockNotAcquired() {
        return Mono.fromRunnable(() -> log.info("[BLOOM-FILTER-INITIALIZER] Another instance is initializing."));
    }

    private Mono<Void> initializeBloomFilter() {
        return bloomFilter.tryInit(expectedInsertions, falseProbability)
                .doOnSuccess(result -> log.info("[BLOOM-FILTER-INITIALIZER] Bloom filter created with {} expected insertions and {} false probability", expectedInsertions, falseProbability))
                .onErrorResume(error -> {
                    log.error("[BLOOM-FILTER-INITIALIZER] Initialization failed", error);
                    return Mono.empty();
                })
                .flatMap(result -> bloomFilter.isExists().flatMap(exists -> Boolean.TRUE.equals(exists) ? populateBloomFilter() : Mono.empty()));
    }

    private Mono<Void> populateBloomFilter() {
        return citizenRepository.findAll()
                .filter(this::hasValidConsent)
                .map(CitizenConsent::getFiscalCode)
                .buffer(100)
                .flatMap(this::addBatchToBloomFilter)
                .then(Mono.fromRunnable(() -> log.info("[BLOOM-FILTER-INITIALIZER] Population complete")));
    }

    private boolean hasValidConsent(CitizenConsent citizen) {
        return citizen.getConsents().values().stream().anyMatch(ConsentDetails::getTppState);
    }

    private Mono<Void> addBatchToBloomFilter(List<String> batch) {
        return Flux.fromIterable(batch)
                .flatMap(bloomFilter::add)
                .then();
    }

    @Scheduled(cron = "0 0 4 * * ?")
    public void resetBloomFilter() {
        acquireLock()
                .flatMap(this::processReset)
                .subscribe();
    }

    private Mono<Void> processReset(Boolean lockAcquired) {
        if (Boolean.FALSE.equals(lockAcquired)) {
            return logLockNotAcquiredForReset();
        }
        return performReset()
                .doFinally(signal -> releaseLock());
    }

    private Mono<Void> logLockNotAcquiredForReset() {
        return Mono.fromRunnable(() -> log.info("[BLOOM-FILTER-INITIALIZER] Another instance is resetting the Bloom Filter."));
    }

    private Mono<Void> performReset() {
        log.info("[BLOOM-FILTER-INITIALIZER] Resetting Bloom Filter...");
        return bloomFilter.delete()
                .then(initializeBloomFilter())
                .doOnSuccess(v -> log.info("[BLOOM-FILTER-INITIALIZER] Bloom Filter reset completed."))
                .doOnError(error -> log.error("[BLOOM-FILTER-INITIALIZER] Error during Bloom Filter reset", error));
    }

    private void releaseLock() {
        redissonClient.getLock(REDIS_LOCK_NAME).unlock();
        log.info("[BLOOM-FILTER-INITIALIZER] Lock released.");
    }

}