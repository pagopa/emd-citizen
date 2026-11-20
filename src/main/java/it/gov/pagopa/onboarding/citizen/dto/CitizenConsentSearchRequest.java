package it.gov.pagopa.onboarding.citizen.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import it.gov.pagopa.onboarding.citizen.validator.RecipientId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class CitizenConsentSearchRequest {

    @RecipientId
    @Schema(type = "string", minLength = RecipientId.MIN_LENGTH, maxLength = RecipientId.MAX_LENGTH)
    private String fiscalCode;
}
