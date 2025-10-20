package it.gov.pagopa.onboarding.citizen.service;


import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilterReactive;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class BloomFilterServiceImpl {

    private final RBloomFilterReactive<String> bloomFilter;

    public BloomFilterServiceImpl(BloomFilterInitializer bloomFilterInitializer) {
        this.bloomFilter = bloomFilterInitializer.getBloomFilter();
    }

    public void add(String value) {
        bloomFilter.add(value)
                .doOnSuccess(result -> {
                    if (Boolean.TRUE.equals(result)) {
                        log.info("[BLOOM-FILTER-SERVICE] Fiscal Code added to bloom filter");
                    } else {
                        log.info("[BLOOM-FILTER-SERVICE] Fiscal Code not added to bloom filter");
                    }
                })
                .subscribe();
    }

    public Mono<String> mightContain(String value) {
        log.info("[BLOOM-FILTER-SERVICE] Bloom filter search request arrived");
        return bloomFilter.contains(value)
                .map(result -> {
                    if (Boolean.TRUE.equals(result)) {
                        log.info("[BLOOM-FILTER-SERVICE] Fiscal Code found");
                        return "OK";
                    } else {
                        log.info("[BLOOM-FILTER-SERVICE] Fiscal Code not found");
                        return "NO CHANNELS ENABLED";
                    }
                });
    }

    /**
     * Check if the value is contained in the bloom filter
     * @param value the value to check
     * @return Mono<Boolean> true if the value is contained, false otherwise
     */
    public Mono<Boolean> contains(String value) {
        log.info("[BLOOM-FILTER-SERVICE] Bloom filter search request arrived");
        return bloomFilter.contains(value);
    }
}

