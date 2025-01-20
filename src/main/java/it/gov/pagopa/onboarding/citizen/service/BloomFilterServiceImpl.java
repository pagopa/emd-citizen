package it.gov.pagopa.onboarding.citizen.service;


import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.stereotype.Service;

import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class BloomFilterServiceImpl implements BloomFilterService {

    private static final String REDDIS_BF_NAME = "emd-bloom-fiter";
    private static final String REDIS_LOCK_NAME = "startup-task-lock";

    RBloomFilter<String> bloomFilter ;
    public BloomFilterServiceImpl(RedissonClient redissonClient) {
        RLock lock = redissonClient.getLock(REDIS_LOCK_NAME);

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
    private RBloomFilter<String> initializeBloomFilter(RedissonClient redissonClient) {
        RBloomFilter<String> filter = redissonClient.getBloomFilter(REDDIS_BF_NAME);
        filter.tryInit(1000000L, 0.01);
        return filter;
    }

    public void add(String value) {
        bloomFilter.add(value);
    }

    public boolean mightContain(String value) {
        return bloomFilter.contains(value);
    }

    //Valutare re-inizializzaione temporizata per rimuovere eventuali elementi
}