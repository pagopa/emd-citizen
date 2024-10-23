package it.gov.pagopa.onboarding.citizen.dto.mapper;


import it.gov.pagopa.onboarding.citizen.dto.CitizenConsentDTO;
import it.gov.pagopa.onboarding.citizen.model.CitizenConsent;
import org.springframework.stereotype.Service;

@Service
public class CitizenConsentObjectToDTOMapper {

    public CitizenConsentDTO map(CitizenConsent citizenConsent){
        return CitizenConsentDTO.builder()
                .hashedFiscalCode(citizenConsent.getHashedFiscalCode())
                .tppState(citizenConsent.getTppState())
                .tppId(citizenConsent.getTppId())
                .creationDate(citizenConsent.getCreationDate())
                .lastUpdateDate(citizenConsent.getLastUpdateDate())
                .build();
    }
}
