package it.gov.pagopa.onboarding.citizen.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = NotBlankUnicodeValidator.class)
@Target( { ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface NotBlankUnicode {
    String message() default "field is required";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
