package it.gov.pagopa.onboarding.citizen.service;

import it.gov.pagopa.onboarding.citizen.dto.CitizenConsentDTO;
import reactor.core.publisher.Mono;

import java.util.List;

public interface CitizenService {

    Mono<CitizenConsentDTO> createCitizenConsent(CitizenConsentDTO citizenConsent);
    Mono<CitizenConsentDTO> updateTppState(String fiscalCode, String tppId, boolean tppState);
    Mono<CitizenConsentDTO> getConsentStatus(String fiscalCode, String tppId);
    Mono<List<String>> getTppEnabledList(String fiscalCode);
    Mono<CitizenConsentDTO> get(String fiscalCode);
}
