package it.gov.pagopa.onboarding.citizen.service;


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
    public BloomFilterServiceImpl(RedissonReactiveClient redissonClient) {
        RLockReactive lock = redissonClient.getLock(REDIS_LOCK_NAME);

        try {
            if (lock.tryLock(0, TimeUnit.SECONDS)) {
                try {
                    this.bloomFilter = initializeBloomFilter(redissonClient);
                    // carico dati dal db
                    log.info("[BLOOM-FILTER-SERVICE] Inizializzazione bloom filter eseguita");
                } finally {
                    lock.unlock();
                }
            } else {
                this.bloomFilter = redissonClient.getBloomFilter(REDDIS_BF_NAME);
                log.info("BLOOM-FILTER-SERVICE] Un'altra replica sta già eseguendo l'inizializzazione bloom filter ");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Errore durante l'acquisizione del lock.", e);
        }
    }
    private RBloomFilterReactive<String> initializeBloomFilter(RedissonReactiveClient redissonClient) {
        RBloomFilterReactive<String> filter = redissonClient.getBloomFilter(REDDIS_BF_NAME);
        filter.tryInit(1000000L, 0.01);
        return filter;
    }

    public void add(String value) {
        bloomFilter.add(value);
    }

    public Mono<Boolean> mightContain(String value) {
        return bloomFilter.contains(value);
    }

    //Valutare re-inizializzaione temporizata per rimuovere eventuali elementi
}