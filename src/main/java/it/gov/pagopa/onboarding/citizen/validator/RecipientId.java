package it.gov.pagopa.onboarding.citizen.validator;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = RecipientId.Validator.class)
@Schema(type = "string", minLength = RecipientId.MIN_LENGTH, maxLength = RecipientId.MAX_LENGTH)
public @interface RecipientId {
    int MIN_LENGTH = 1;
    int MAX_LENGTH = 100;
    String message() default "The fiscalCode field must be between 1 and 100 characters";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<RecipientId, String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null) {
                return false;
            }
            int length = value.codePointCount(0, value.length());
            return length >= MIN_LENGTH && length <= MAX_LENGTH;
        }
    }
}
