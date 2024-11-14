package it.gov.pagopa.onboarding.citizen.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.Map;

import static it.gov.pagopa.onboarding.citizen.constants.CitizenConstants.ValidationRegex.FISCAL_CODE_STRUCTURE_REGEX;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class CitizenConsentDTO {
    @JsonAlias("fiscalCode")
    @NotBlank(message = "Fiscal Code must not be blank")
    @Pattern(regexp = FISCAL_CODE_STRUCTURE_REGEX,
            message = "Fiscal Code must be 11 digits or up to 16 alphanumeric characters")
    private String fiscalCode;
    private Map<String, ConsentDTO> consents;

    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConsentDTO {
        private Boolean tppState;
        private LocalDateTime tcDate;
    }
}
