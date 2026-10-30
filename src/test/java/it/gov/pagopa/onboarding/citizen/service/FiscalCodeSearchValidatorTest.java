package it.gov.pagopa.onboarding.citizen.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FiscalCodeSearchValidatorTest {

    private final FiscalCodeSearchValidator validator = new FiscalCodeSearchValidator();

    @Test
    void acceptsAndNormalizesPrefix() {
        assertEquals("RSSMRA", validator.validateAndNormalize("rssmra"));
    }

    @Test
    void acceptsValidCompleteFiscalCode() {
        assertEquals("RSSMRA85T10A562S", validator.validateAndNormalize("rssmra85t10a562s"));
    }

    @Test
    void completeFiscalCodeValidationRejectsPrefixes() {
        assertThrows(ResponseStatusException.class, () -> validator.validateCompleteFiscalCode("RSSMRA"));
    }

    @Test
    void rejectsTooShortInput() {
        assertBadRequest("AB");
    }

    @Test
    void rejectsNonAlphanumericInput() {
        assertBadRequest("RSS MRA");
    }

    @Test
    void rejectsMalformedCompleteFiscalCode() {
        assertBadRequest("RSSMRA85T10A5621");
    }

    @Test
    void rejectsInputLongerThanFiscalCode() {
        assertBadRequest("RSSMRA85T10A562S1");
    }

    @Test
    void acceptsCursorForMatchingPrefix() {
        assertEquals("RSSMRA85T10A562S",
                validator.validateAndNormalizeCursor("rssmra85t10a562s", "RSSMRA"));
    }

    @Test
    void rejectsInvalidOrUnrelatedCursor() {
        assertThrows(ResponseStatusException.class,
                () -> validator.validateAndNormalizeCursor("not-a-fiscal-code", "RSSMRA"));
        assertThrows(ResponseStatusException.class,
                () -> validator.validateAndNormalizeCursor("BNCLGU80A01H501U", "RSSMRA"));
    }

    @Test
    void rejectsInvalidPageSize() {
        assertThrows(ResponseStatusException.class, () -> validator.validateSize(0));
        assertThrows(ResponseStatusException.class, () -> validator.validateSize(101));
    }

    private void assertBadRequest(String value) {
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> validator.validateAndNormalize(value)).getStatusCode().value());
    }
}
