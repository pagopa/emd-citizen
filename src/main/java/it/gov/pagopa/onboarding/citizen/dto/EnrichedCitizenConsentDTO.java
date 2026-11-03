package it.gov.pagopa.onboarding.citizen.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class EnrichedCitizenConsentDTO {

	private String fiscalCode;
	private Map<String, EnrichedConsentDTO> consents;

	@Data
	@SuperBuilder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class EnrichedConsentDTO {
		private Boolean tppState;
		private LocalDateTime tcDate;
		private String entityId;
		private String businessName;
	}
}

