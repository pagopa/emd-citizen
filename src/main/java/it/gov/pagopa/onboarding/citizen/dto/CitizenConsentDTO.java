package it.gov.pagopa.onboarding.citizen.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class CitizenConsentDTO {
    @JsonAlias("fiscalCode")
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
