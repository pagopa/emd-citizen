package it.gov.pagopa.onboarding.citizen.dto.mapper;

import it.gov.pagopa.onboarding.citizen.dto.CitizenConsentDTO;
import it.gov.pagopa.onboarding.citizen.dto.CitizenConsentDTO.ConsentDTO;
import it.gov.pagopa.onboarding.citizen.model.CitizenConsent;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>Mapper service converting domain {@link CitizenConsent} aggregates into transport {@link CitizenConsentDTO}.</p>
 *
 * <p>Performs a deep copy of the consents map, transforming each domain {@code Consent} into a {@link ConsentDTO}.</p>
 *
 * <p>Inverse operation of {@code CitizenConsentDTOToObjectMapper}.</p>
 * */
@Service
public class CitizenConsentObjectToDTOMapper {

    /**
     * <p>Converts a domain {@link CitizenConsent} into a {@link CitizenConsentDTO}.</p>
     *
     * <p>Maps the entire consents map: {@code Map<String, Consent>} → {@code Map<String, ConsentDTO>}.</p>
     *
     * @param citizenConsent source domain aggregate (must not be {@code null})
     * @return DTO with fiscal code and mapped consent details
     * @throws NullPointerException if {@code citizenConsent} or {@code consents} map is {@code null}
     */
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
