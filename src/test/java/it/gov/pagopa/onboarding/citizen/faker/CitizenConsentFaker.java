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
                .tppState(bias)
                .tcDate(LocalDateTime.now())
                .build();

        consents.put("tppId", consentDetails);

        return CitizenConsent.builder()
                .fiscalCode("fiscalCode")
                .consents(consents)
                .build();
    }
}
