package it.gov.pagopa.onboarding.citizen.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FiscalCodeSearchValidatorTest {

    private final FiscalCodeSearchValidator validator = new FiscalCodeSearchValidator();

    @Test
    void acceptsCursorForMatchingPrefix() {
        assertEquals("a. b", validator.validateAndNormalizeCursor("a. b", "a."));
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

}
