package it.gov.pagopa.onboarding.citizen.service;

import reactor.core.publisher.Mono;

/**
 * <p>Probabilistic service for fast citizen consent lookups using a Redis-backed Bloom Filter.</p>
 *
 * <p>Used as a first-layer filter before MongoDB queries to reduce database load:
 * <ul>
 *   <li><b>"false"</b> → Fiscal code definitely absent (skip DB query)</li>
 *   <li><b>"true"</b> → Fiscal code might exist (verify with DB)</li>
 * </ul>
 *
 * @see BloomFilterInitializer
 */
public interface BloomFilterService {

    /**
     * <p>Checks if a fiscal code might be present in the Bloom Filter.</p>
     *
     * @param fiscalCode plain fiscal code
     * @return {@code "true"} if might exist (requires DB check), {@code "false"} if definitely absent
     */
     Mono<String> mightContain(String fiscalCode);
}
