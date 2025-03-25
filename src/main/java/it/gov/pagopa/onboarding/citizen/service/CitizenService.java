package it.gov.pagopa.onboarding.citizen.service;

import it.gov.pagopa.onboarding.citizen.dto.CitizenConsentDTO;
import reactor.core.publisher.Mono;

import java.util.List;

public interface CitizenService {

    Mono<CitizenConsentDTO> createCitizenConsent(String fiscalCode, String tppId);
    Mono<CitizenConsentDTO> switchState(String fiscalCode, String tppId);
    Mono<CitizenConsentDTO> getCitizenConsentStatus(String fiscalCode, String tppId);
    Mono<List<String>> getTppEnabledList(String fiscalCode);
    Mono<CitizenConsentDTO> getCitizenConsentsList(String fiscalCode);
    Mono<CitizenConsentDTO> getCitizenConsentsListEnabled(String fiscalCode);
    Mono<List<CitizenConsentDTO>> getCitizenEnabled(String tppId);
    Mono<CitizenConsentDTO> deleteCitizenConsent(String fiscalCode);
}
