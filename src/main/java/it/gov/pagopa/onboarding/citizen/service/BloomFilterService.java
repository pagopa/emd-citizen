package it.gov.pagopa.onboarding.citizen.service;

import reactor.core.publisher.Mono;

public interface BloomFilterService {

     Mono<Boolean> mightContain(String hashedFiscalCode);
}
