package it.gov.pagopa.onboarding.citizen.service;

import it.gov.pagopa.onboarding.citizen.model.ConsentDetails;
import it.gov.pagopa.onboarding.citizen.repository.CitizenRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilterReactive;
import org.redisson.api.RLockReactive;
import org.redisson.api.RedissonReactiveClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class BloomFilterInitializer {

    private static final String REDDIS_BF_NAME = "emd-bloom-fiter";
    private static final String REDIS_LOCK_NAME = "startup-task-lock";

    @Getter
    private final RBloomFilterReactive<String> bloomFilter;
    private final RedissonReactiveClient redissonClient;
    private final CitizenRepository citizenRepository;

    public BloomFilterInitializer(RedissonReactiveClient redissonClient, CitizenRepository citizenRepository) {
        this.redissonClient = redissonClient;
        this.citizenRepository = citizenRepository;
        this.bloomFilter = redissonClient.getBloomFilter(REDDIS_BF_NAME);
    }

    @PostConstruct
    public void initializer() {
        RLockReactive lock = redissonClient.getLock(REDIS_LOCK_NAME);

        lock.tryLock(10, TimeUnit.SECONDS)
                .flatMap(lockAcquired -> {
                    if (Boolean.TRUE.equals(lockAcquired)) {
                        return initializeBloomFilter();
                    } else {
                        log.info("[BLOOM-FILTER-INITIALIZER] Another instance is initializing the bloom filter.");
                        return Mono.empty();
                    }
                })
                .doFinally(signal -> unlockLock(lock))
                .subscribe();
    }

    private Mono<Void> initializeBloomFilter() {
        return bloomFilter.tryInit(1000L, 0.01)
                .doOnSuccess(result -> log.info("[BLOOM-FILTER-INITIALIZER] Bloom filter created"))
                .doOnError(result -> log.info("[BLOOM-FILTER-INITIALIZER] Bloom filter creation failed"))
                .flatMap(result -> populateBloomFilter())
                .then();
    }

    private Mono<Void> populateBloomFilter() {
        return citizenRepository.findAll()
                .map(citizen -> citizen.getConsents().values().stream()
                        .anyMatch(ConsentDetails::getTppState) ? citizen.getFiscalCode() : "")
                .filter(fiscalCode -> !fiscalCode.isEmpty())
                .flatMap(fiscalCode -> bloomFilter.add(fiscalCode).then())
                .doOnComplete(() -> log.info("[BLOOM-FILTER-INITIALIZER] Bloom filter population complete"))
                .doOnError(error -> log.error("[BLOOM-FILTER-INITIALIZER] Error populating bloom filter", error))
                .then();
    }

    private void unlockLock(RLockReactive lock) {
        lock.unlock()
                .doOnSuccess(error -> log.error("[BLOOM-FILTER-INITIALIZER] Lock unlock"))
                .doOnError(error -> log.error("[BLOOM-FILTER-INITIALIZER] Failed to unlock", error))
                .subscribe();
    }
}

