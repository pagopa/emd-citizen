package it.gov.pagopa.onboarding.citizen.service;

import it.gov.pagopa.onboarding.citizen.validator.RecipientId;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class FiscalCodeSearchValidator {

    public void validateSize(int size) {
        if (size < 1 || size > 100) {
            throw badRequest("La dimensione della pagina deve essere compresa tra 1 e 100.");
        }
    }

    public String validateAndNormalizeCursor(String cursor, String normalizedFiscalCode) {
        if (cursor == null) {
            return null;
        }
        if (!new RecipientId.Validator().isValid(cursor, null)) {
            throw badRequest("Il cursore deve contenere da 1 a 100 caratteri.");
        }
        if (!cursor.startsWith(normalizedFiscalCode)) {
            throw badRequest("Il cursore non appartiene al prefisso cercato.");
        }
        return cursor;
    }

    private ResponseStatusException badRequest(String reason) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
    }
}
