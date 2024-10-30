package it.gov.pagopa.onboarding.citizen.faker;

import it.gov.pagopa.onboarding.citizen.dto.CitizenConsentDTO;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class CitizenConsentDTOFaker {

    private CitizenConsentDTOFaker() {}

    public static CitizenConsentDTO mockInstance(Boolean bias) {
        Map<String, CitizenConsentDTO.ConsentDTO> consents = new HashMap<>();

        consents.put("tppId", new CitizenConsentDTO.ConsentDTO(bias, bias, LocalDateTime.now(), LocalDateTime.now()));

        return CitizenConsentDTO.builder()
                .hashedFiscalCode("hashedFiscalCode")
                .consents(consents)
                .build();
    }
}
