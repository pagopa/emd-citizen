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
                .tc(consentDTO.getTc())
                .tppState(consentDTO.getTppState())
                .creationDate(consentDTO.getCreationDate())
                .lastTcUpdateDate(consentDTO.getLastUpdateDate())
                .build()));

        return CitizenConsent.builder()
                .hashedFiscalCode(citizenConsentDTO.getHashedFiscalCode())
                .consents(consents)
                .build();
    }
}
