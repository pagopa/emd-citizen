package it.gov.pagopa.onboarding.citizen.service;

import it.gov.pagopa.onboarding.citizen.dto.CitizenConsentDTO;
import reactor.core.publisher.Mono;

import java.util.List;

public interface CitizenService {

    Mono<CitizenConsentDTO> createCitizenConsent(CitizenConsentDTO citizenConsent);
    Mono<CitizenConsentDTO> updateChannelState(String fiscalCode, String tppId, boolean tppState);
    Mono<CitizenConsentDTO> getConsentStatus(String fiscalCode, String tppId);
    Mono<List<CitizenConsentDTO>> getListEnabledConsents(String fiscalCode);
    Mono<List<CitizenConsentDTO>> getListAllConsents(String fiscalCode);
}
