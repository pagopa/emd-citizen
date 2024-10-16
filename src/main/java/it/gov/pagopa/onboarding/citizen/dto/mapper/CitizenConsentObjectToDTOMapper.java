package it.gov.pagopa.onboarding.citizen.dto.mapper;


import it.gov.pagopa.onboarding.citizen.dto.CitizenConsentDTO;
import it.gov.pagopa.onboarding.citizen.model.CitizenConsent;
import org.springframework.stereotype.Service;

@Service
public class CitizenConsentObjectToDTOMapper {

    public CitizenConsentDTO map(CitizenConsent citizenConsent){
        return CitizenConsentDTO.builder()
                .tppState(citizenConsent.getTppState())
                .tppId(citizenConsent.getTppId())
                .hashedFiscalCode(citizenConsent.getHashedFiscalCode())
                .userId(citizenConsent.getUserId())
                .build();
    }
}
