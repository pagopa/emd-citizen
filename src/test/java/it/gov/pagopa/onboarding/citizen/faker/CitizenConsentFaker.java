package it.gov.pagopa.onboarding.citizen.faker;


import it.gov.pagopa.onboarding.citizen.model.CitizenConsent;

import java.time.LocalDateTime;

public class CitizenConsentFaker {

    private CitizenConsentFaker(){}
    public static CitizenConsent mockInstance(Boolean bias) {
        return CitizenConsent.builder()
                .tppId("tppId")
                .tppState(bias)
                .userId("userId")
                .hashedFiscalCode("hashedFiscalCode")
                .creationDate(LocalDateTime.now())
                .lastUpdateDate(LocalDateTime.now())
                .build();
    }
}
