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
                .tc(consentDetails.getTc())
                .tppState(consentDetails.getTppState())
                .creationDate(consentDetails.getCreationDate())
                .lastUpdateDate(consentDetails.getLastUpdateDate())
                .build()));

        return CitizenConsentDTO.builder()
                .hashedFiscalCode(citizenConsent.getHashedFiscalCode())
                .consents(consentsDTO)
                .build();
    }
}
