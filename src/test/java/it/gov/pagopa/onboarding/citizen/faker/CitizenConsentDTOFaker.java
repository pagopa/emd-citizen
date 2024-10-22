package it.gov.pagopa.onboarding.citizen.faker;


import it.gov.pagopa.onboarding.citizen.dto.CitizenConsentDTO;

import java.time.LocalDateTime;

public class CitizenConsentDTOFaker {

    private CitizenConsentDTOFaker(){}
    public static CitizenConsentDTO mockInstance(Boolean bias) {
        return CitizenConsentDTO.builder()
                .tppId("tppId")
                .tppState(bias)
                .hashedFiscalCode("hashedFiscalCode")
                .creationDate(LocalDateTime.now())
                .lastUpdateDate(LocalDateTime.now())
                .build();

    }
}
