package it.gov.pagopa.onboarding.citizen.faker;

import it.gov.pagopa.onboarding.citizen.dto.TppDTO;

public class TppDTOFaker {
    private TppDTOFaker(){}
    public static TppDTO mockInstance() {
        return TppDTO.builder()
                .tppId("id")
                .build();
    }
}
