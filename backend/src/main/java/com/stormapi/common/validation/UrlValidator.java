package com.stormapi.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.net.URI;

/**
 * Validates that a string is a well-formed HTTP or HTTPS URL.
 * Null values pass validation (use @NotBlank to enforce non-null).
 */
public class UrlValidator implements ConstraintValidator<ValidUrl, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true; // null handling delegated to @NotBlank
        }

        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme();
            String host = uri.getHost();

            // Must have http or https scheme
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                return false;
            }

            // Must have a host
            return host != null && !host.isBlank();

        } catch (IllegalArgumentException e) {
            return false;
        }
    }

}
