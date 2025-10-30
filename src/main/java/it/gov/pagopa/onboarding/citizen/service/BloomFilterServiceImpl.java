package it.gov.pagopa.onboarding.citizen.service;


import it.gov.pagopa.common.utils.Utils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilterReactive;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class BloomFilterServiceImpl implements BloomFilterService {

    @Getter
    private final RBloomFilterReactive<String> bloomFilter;

    public BloomFilterServiceImpl(BloomFilterInitializer bloomFilterInitializer) {
        this.bloomFilter = bloomFilterInitializer.getBloomFilter();
    }

    /**
     * <p>Adds a fiscal code to the Bloom Filter.</p>
     *
     * @param value the fiscal code to add (plain text, will be hashed for logging)
     * @return {@code Mono<Void>} completing when the value is added
     */
    public Mono<Void> add(String value) {
        return bloomFilter.add(value)
                .doOnNext(result -> {
                    if (Boolean.TRUE.equals(result)) {
                        log.info("[BLOOM-FILTER-SERVICE] Fiscal Code {} added to bloom filter", Utils.createSHA256(value));
                    } else {
                        log.info("[BLOOM-FILTER-SERVICE] Fiscal Code {} not added to bloom filter", Utils.createSHA256(value));
                    }
                })
                .then();
    }

    /**
     * {@inheritDoc}
     *
     * <p><b>Implementation note:</b> Returns string literals for backward compatibility with existing API contracts:</p>
     * <ul>
     *   <li><code>"OK"</code> → Fiscal code might exist (proceed with database verification)</li>
     *   <li><code>"NO CHANNELS ENABLED"</code> → Fiscal code definitely absent (skip database query)</li>
     * </ul>
     *
     * <p>For internal usage, consider using {@link #contains(String)} which returns a boolean.</p>
     */
    @Override
    public Mono<String> mightContain(String fiscalCode) {
        log.info("[BLOOM-FILTER-SERVICE] Bloom filter search request arrived");
        return bloomFilter.contains(fiscalCode)
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
     * <p>Checks if a fiscal code is present in the Bloom Filter (boolean response).</p>
     *
     * <p>Preferred over {@link #mightContain(String)} for internal usage as it returns
     * a strongly-typed boolean instead of string literals.</p>
     *
     * @param value the fiscal code to check
     * @return {@code Mono<Boolean>} emitting {@code true} if might exist, {@code false} if definitely absent
     */
    public Mono<Boolean> contains(String value) {
        log.info("[BLOOM-FILTER-SERVICE] Bloom filter search request arrived");
        return bloomFilter.contains(value);
    }
}

