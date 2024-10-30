package it.gov.pagopa.onboarding.citizen.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CitizenConsentStateUpdateDTO {
    private String hashedFiscalCode;
    private String tppId;
    private Boolean tppState;
}
