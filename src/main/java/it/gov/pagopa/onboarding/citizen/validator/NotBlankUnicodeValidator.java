package it.gov.pagopa.onboarding.citizen.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class NotBlankUnicodeValidator implements ConstraintValidator<NotBlankUnicode, String> {
  private static final Pattern BLANK_PATTERN = Pattern.compile("^[\\s\\u00A0\\u200B\\u2000-\\u200A\\u202F\\u205F\\u3000]+$");

  @Override
  public boolean isValid(String value, ConstraintValidatorContext ctx) {
    if (value == null) return false;
    if (value.isEmpty()) return false;
    return !BLANK_PATTERN.matcher(value).matches();
  }
}