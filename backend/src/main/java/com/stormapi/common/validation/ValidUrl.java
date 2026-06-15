package com.stormapi.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom validation constraint that ensures a string is a valid HTTP or HTTPS URL.
 * Null values are considered valid (use @NotBlank to enforce non-null separately).
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UrlValidator.class)
@Documented
public @interface ValidUrl {

    String message() default "Invalid URL format. Must be a valid HTTP or HTTPS URL.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
