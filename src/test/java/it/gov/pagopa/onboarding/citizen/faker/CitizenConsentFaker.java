package it.gov.pagopa.onboarding.citizen.faker;

import it.gov.pagopa.onboarding.citizen.model.CitizenConsent;
import it.gov.pagopa.onboarding.citizen.model.ConsentDetails;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class CitizenConsentFaker {

    private CitizenConsentFaker() {}

    public static CitizenConsent mockInstance(Boolean bias) {
        Map<String, ConsentDetails> consents = new HashMap<>();

        ConsentDetails consentDetails = ConsentDetails.builder()
                .tc(bias)
                .tppState(bias)
                .creationDate(LocalDateTime.now())
                .lastTcUpdateDate(LocalDateTime.now())
                .lastUpdateDate(LocalDateTime.now())
                .build();

        consents.put("tppId", consentDetails);

        return CitizenConsent.builder()
                .hashedFiscalCode("hashedFiscalCode")
                .consents(consents)
                .build();
    }
}
