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
    public void initialize() {
        RLockReactive lock = redissonClient.getLock(REDIS_LOCK_NAME);
        lock.tryLock(0, TimeUnit.SECONDS)
                .flatMap(lockAcquired -> {
                    if (Boolean.TRUE.equals(lockAcquired)) {
                        return bloomFilter.tryInit(1000L, 0.01)
                                .doOnSuccess(result -> log.info("[BLOOM-FILTER-INITIALIZER] Bloom filter created"))
                                .thenMany(citizenRepository.findAll()
                                        .flatMap(citizenConsent -> {
                                            boolean hasTppState = citizenConsent.getConsents().values().stream()
                                                    .anyMatch(ConsentDetails::getTppState);
                                            if (hasTppState) {
                                                return bloomFilter.add(citizenConsent.getFiscalCode());
                                            }
                                            return Mono.empty();
                                        })
                                )
                                .then(lock.unlock())
                                .doFinally(signal -> log.info("[BLOOM-FILTER-INITIALIZER] Bloom filter initialized"))
                                .then();
                    } else {
                        log.info("[BLOOM-FILTER-INITIALIZER] Another instance is initializing the Bloom Filter");
                        return Mono.empty();
                    }
                })
                .doOnError(error -> log.error("[BLOOM-FILTER-INITIALIZER] Initialization failed", error))
                .block();
    }

}
