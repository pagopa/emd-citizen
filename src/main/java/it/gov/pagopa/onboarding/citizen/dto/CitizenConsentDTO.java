package it.gov.pagopa.onboarding.citizen.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class CitizenConsentDTO {
    private String hashedFiscalCode;
    private String tppId;
    private Boolean tppState;
    private String userId;
    private LocalDateTime creationDate;
    private LocalDateTime lastUpdateDate;
}
