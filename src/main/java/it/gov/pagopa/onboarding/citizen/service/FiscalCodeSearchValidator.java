package it.gov.pagopa.onboarding.citizen.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class FiscalCodeSearchValidator {

    private static final Pattern ALPHANUMERIC = Pattern.compile("^[A-Za-z0-9]+$");
    private static final Pattern COMPLETE_FISCAL_CODE = Pattern.compile("^[A-Za-z]{6}[0-9]{2}[A-Za-z]{1}[0-9]{2}[A-Za-z]{1}[0-9]{3}[A-Za-z]{1}$");

    public String validateAndNormalize(String fiscalCode) {
        if (fiscalCode == null || fiscalCode.length() < 3) {
            throw badRequest("La ricerca deve contenere almeno 3 caratteri.");
        }
        if (!ALPHANUMERIC.matcher(fiscalCode).matches()) {
            throw badRequest("Sono ammessi esclusivamente caratteri alfanumerici.");
        }
        if (fiscalCode.length() > 16) {
            throw badRequest("La ricerca non può superare i 16 caratteri.");
        }
        if (fiscalCode.length() == 16 && !COMPLETE_FISCAL_CODE.matcher(fiscalCode).matches()) {
            throw badRequest("Il Codice Fiscale completo non rispetta il formato previsto.");
        }
        return fiscalCode.toUpperCase(Locale.ROOT);
    }

    public void validateSize(int size) {
        if (size < 1 || size > 100) {
            throw badRequest("La dimensione della pagina deve essere compresa tra 1 e 100.");
        }
    }

    public String validateAndNormalizeCursor(String cursor, String normalizedFiscalCode) {
        if (cursor == null) {
            return null;
        }
        if (!COMPLETE_FISCAL_CODE.matcher(cursor).matches()) {
            throw badRequest("Il cursore deve essere un Codice Fiscale completo valido.");
        }
        String normalizedCursor = cursor.toUpperCase(Locale.ROOT);
        if (!normalizedCursor.startsWith(normalizedFiscalCode)) {
            throw badRequest("Il cursore non appartiene al prefisso cercato.");
        }
        return normalizedCursor;
    }

    private ResponseStatusException badRequest(String reason) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
    }
}
