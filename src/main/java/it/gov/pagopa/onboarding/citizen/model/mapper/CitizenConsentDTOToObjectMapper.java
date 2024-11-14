package it.gov.pagopa.onboarding.citizen.model.mapper;

import it.gov.pagopa.onboarding.citizen.dto.CitizenConsentDTO;
import it.gov.pagopa.onboarding.citizen.model.CitizenConsent;
import it.gov.pagopa.onboarding.citizen.model.ConsentDetails;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class CitizenConsentDTOToObjectMapper {

    public CitizenConsent map(CitizenConsentDTO citizenConsentDTO) {
        Map<String, ConsentDetails> consents = new HashMap<>();

        citizenConsentDTO.getConsents().forEach((tppId, consentDTO) -> consents.put(tppId, ConsentDetails.builder()
                .tppState(consentDTO.getTppState())
                .tcDate(consentDTO.getTcDate())
                .build()));

        return CitizenConsent.builder()
                .fiscalCode(citizenConsentDTO.getFiscalCode())
                .consents(consents)
                .build();
    }
}
