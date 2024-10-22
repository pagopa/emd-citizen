package it.gov.pagopa.onboarding.citizen.model.mapper;


import it.gov.pagopa.onboarding.citizen.dto.CitizenConsentDTO;
import it.gov.pagopa.onboarding.citizen.model.CitizenConsent;
import org.springframework.stereotype.Service;

@Service
public class CitizenConsentDTOToObjectMapper {

    public CitizenConsent map(CitizenConsentDTO citizenConsentDTO){
        return CitizenConsent.builder()
                .tppState(true)
                .tppId(citizenConsentDTO.getTppId())
                .hashedFiscalCode(citizenConsentDTO.getHashedFiscalCode())
                .build();
    }
}
