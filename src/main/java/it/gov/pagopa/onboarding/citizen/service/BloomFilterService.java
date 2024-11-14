package it.gov.pagopa.onboarding.citizen.service;

public interface BloomFilterService {

     boolean mightContain(String hashedFiscalCode);
}
