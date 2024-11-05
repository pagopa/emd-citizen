package it.gov.pagopa.onboarding.citizen.dto.mapper;

import it.gov.pagopa.onboarding.citizen.dto.CitizenConsentDTO;
import it.gov.pagopa.onboarding.citizen.dto.CitizenConsentDTO.ConsentDTO;
import it.gov.pagopa.onboarding.citizen.model.CitizenConsent;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class CitizenConsentObjectToDTOMapper {

    public CitizenConsentDTO map(CitizenConsent citizenConsent) {
        Map<String, ConsentDTO> consentsDTO = new HashMap<>();

        citizenConsent.getConsents().forEach((tppId, consentDetails) -> consentsDTO.put(tppId, ConsentDTO.builder()
                .tppState(consentDetails.getTppState())
                .tcDate(consentDetails.getTcDate())
                .build()));

        return CitizenConsentDTO.builder()
                .fiscalCode(citizenConsent.getFiscalCode())
                .consents(consentsDTO)
                .build();
    }
}
