package it.gov.pagopa.onboarding.citizen.service;

import reactor.core.publisher.Mono;

public interface BloomFilterService {

     Mono<String> mightContain(String hashedFiscalCode);
}
