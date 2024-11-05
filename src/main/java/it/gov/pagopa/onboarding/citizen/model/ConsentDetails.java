package it.gov.pagopa.onboarding.citizen.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@SuperBuilder
public class ConsentDetails {
    private Boolean tppState;
    private LocalDateTime tcDate;
}
