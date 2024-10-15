package it.gov.pagopa.onboarding.citizen.service;

import it.gov.pagopa.onboarding.citizen.dto.CitizenConsentDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CitizenService {

    Mono<CitizenConsentDTO> createCitizenConsent(CitizenConsentDTO citizenConsent);
    Mono<CitizenConsentDTO> updateChannelState(String hashedFiscalCode, String tppId, boolean tppState);
    Mono<CitizenConsentDTO> getConsentStatus(String fiscalCode, String tppId);
    Flux<CitizenConsentDTO> getListEnabledConsents(String fiscalCode);
    Flux<CitizenConsentDTO> getListAllConsents(String fiscalCode);
}
