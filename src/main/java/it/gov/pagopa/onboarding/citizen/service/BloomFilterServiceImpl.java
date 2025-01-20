package it.gov.pagopa.onboarding.citizen.service;


import it.gov.pagopa.onboarding.citizen.model.ConsentDetails;
import it.gov.pagopa.onboarding.citizen.repository.CitizenRepository;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.*;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class BloomFilterServiceImpl implements BloomFilterService {

    private static final String REDDIS_BF_NAME = "emd-bloom-fiter";
    private static final String REDIS_LOCK_NAME = "startup-task-lock";

    RBloomFilterReactive<String> bloomFilter ;
    public BloomFilterServiceImpl(RedissonReactiveClient redissonClient, CitizenRepository citizenRepository) {
        RLockReactive lock = redissonClient.getLock(REDIS_LOCK_NAME);
        lock.tryLock(0, TimeUnit.SECONDS)
                .subscribe(lockAcquired -> {
                    if (Boolean.TRUE.equals(lockAcquired)) {
                        try {
                            this.bloomFilter = initializeBloomFilter(redissonClient);
                            citizenRepository
                                            .findAll()
                                            .map(citizenConsent -> citizenConsent.getConsents().values().stream()
                                                        .anyMatch(ConsentDetails::getTppState) ? citizenConsent.getFiscalCode() : ""
                                            )
                                            .doOnNext(fiscalCode ->  {
                                                if (!fiscalCode.isEmpty()) this.bloomFilter.add(fiscalCode);
                                            })
                                            .subscribe();
                            log.info("[BLOOM-FILTER-SERVICE] Inizializzazione bloom filter eseguita");
                        } finally {
                            lock.unlock();
                        }
                    } else {
                        this.bloomFilter = redissonClient.getBloomFilter(REDDIS_BF_NAME);
                        log.info("[BLOOM-FILTER-SERVICE] Un'altra replica sta già eseguendo l'inizializzazione del bloom filter ");
                    }
                });
    }
    private RBloomFilterReactive<String> initializeBloomFilter(RedissonReactiveClient redissonClient) {
        RBloomFilterReactive<String> filter = redissonClient.getBloomFilter(REDDIS_BF_NAME);
        filter.tryInit(1000000L, 0.01);
        return filter;
    }

    public void add(String value) {
        bloomFilter.add(value).doOnSuccess(result -> {
            if(result)
                log.info("[BLOOM-FILTER-SERVICE] Fiscal Code added to bloom filter");
            else
                log.info("[BLOOM-FILTER-SERVICE] Fiscal Code not added to bloom filter");
        })
        .subscribe();
    }

    public Mono<String> mightContain(String value) {
        return bloomFilter.contains(value).map(result -> {
            if (Boolean.TRUE.equals(result)) {
                log.info("[BLOOM-FILTER-SERVICE] Fiscal Code found");
                return "OK";
            } else {
                log.info("[BLOOM-FILTER-SERVICE] Fiscal Code not found");
                return "NO CHANNELS ENABLED";
            }
        });
    }
}