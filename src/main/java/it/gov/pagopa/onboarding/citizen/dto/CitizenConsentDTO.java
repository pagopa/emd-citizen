package it.gov.pagopa.onboarding.citizen.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
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
    @JsonAlias("fiscalCode")
    private String hashedFiscalCode;
    private String tppId;
    private Boolean tppState;
    private LocalDateTime creationDate;
    private LocalDateTime lastUpdateDate;
}
