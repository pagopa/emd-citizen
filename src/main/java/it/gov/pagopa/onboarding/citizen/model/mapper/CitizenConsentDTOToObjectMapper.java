package it.gov.pagopa.onboarding.citizen.model.mapper;

import it.gov.pagopa.onboarding.citizen.dto.CitizenConsentDTO;
import it.gov.pagopa.onboarding.citizen.model.CitizenConsent;
import it.gov.pagopa.onboarding.citizen.model.ConsentDetails;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>Mapper service converting transport {@link CitizenConsentDTO} into domain {@link CitizenConsent} aggregates.</p>
 *
 * <p>Performs a deep copy of the consents map, transforming each {@link CitizenConsentDTO.ConsentDTO} into a {@link ConsentDetails}.</p>
 *
 * <p>Inverse operation of {@code CitizenConsentObjectToDTOMapper}.</p>
 */
@Service
public class CitizenConsentDTOToObjectMapper {

    /**
     * <p>Converts a transport {@link CitizenConsentDTO} into a domain {@link CitizenConsent}.</p>
     *
     * <p>Maps the entire consents map: {@code Map<String, ConsentDTO>} → {@code Map<String, ConsentDetails>}.</p>
     *
     * @param citizenConsentDTO source DTO containing fiscal code and consent details (must not be {@code null})
     * @return domain aggregate with fiscal code and mapped consent details
     * @throws NullPointerException if {@code citizenConsentDTO} or {@code consents} map is {@code null}
     */
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
