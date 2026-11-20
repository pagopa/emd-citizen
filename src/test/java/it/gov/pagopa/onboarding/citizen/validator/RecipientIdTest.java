package it.gov.pagopa.onboarding.citizen.validator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipientIdTest {
    private final RecipientId.Validator validator = new RecipientId.Validator();

    @Test
    void acceptsAnyCharactersWithinBounds() {
        assertTrue(validator.isValid(" ", null));
        assertTrue(validator.isValid("\n/.", null));
        assertTrue(validator.isValid("a".repeat(100), null));
        assertTrue(validator.isValid("😀".repeat(100), null));
    }

    @Test
    void rejectsMissingEmptyAndTooLongValues() {
        assertFalse(validator.isValid(null, null));
        assertFalse(validator.isValid("", null));
        assertFalse(validator.isValid("a".repeat(101), null));
        assertFalse(validator.isValid("😀".repeat(101), null));
    }
}
