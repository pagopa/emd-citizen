package it.gov.pagopa.onboarding.citizen.dto;

import it.gov.pagopa.onboarding.citizen.constants.CitizenConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class CitizenConsentSearchRequest {

    @NotBlank(message = "Fiscal Code must not be blank")
    @Pattern(
            regexp = CitizenConstants.ValidationRegex.COMPLETE_FISCAL_CODE,
            message = "Fiscal Code format is invalid"
    )
    private String fiscalCode;
}
