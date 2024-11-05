package it.gov.pagopa.onboarding.citizen.faker;

import it.gov.pagopa.onboarding.citizen.dto.CitizenConsentStateUpdateDTO;

public class CitizenConsentStateUpdateDTOFaker {

    private CitizenConsentStateUpdateDTOFaker() {}

    public static CitizenConsentStateUpdateDTO mockInstance(Boolean tppState) {
        return CitizenConsentStateUpdateDTO.builder()
                .fiscalCode("hashedFiscalCode")
                .tppId("tppId")
                .tppState(tppState)
                .build();
    }
}
