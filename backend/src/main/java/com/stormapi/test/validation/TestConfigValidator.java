package com.stormapi.test.validation;

import com.stormapi.common.exception.InvalidTestConfigException;
import com.stormapi.test.dto.CreateTestRequest;
import com.stormapi.test.model.HttpMethod;
import com.stormapi.test.model.TestType;

import java.util.Set;

/**
 * Cross-field business rule validator for test configurations.
 * Called after Jakarta Validation passes, before entity creation.
 *
 * Rules enforce type-specific field requirements and logical constraints
 * that cannot be expressed with single-field annotations.
 */
public final class TestConfigValidator {

    private static final Set<HttpMethod> BODY_REQUIRED_METHODS = Set.of(
            HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH
    );

    private TestConfigValidator() {}

    /**
     * Validates cross-field business rules.
     *
     * @param request the validated request DTO
     * @throws InvalidTestConfigException if any cross-field rule is violated
     */
    public static void validate(CreateTestRequest request) {
        validateRampUp(request);
        validateTypeSpecificFields(request);
        validateBodyForMethod(request);
    }

    private static void validateRampUp(CreateTestRequest request) {
        if (request.rampUpSeconds() >= request.durationSeconds()) {
            throw new InvalidTestConfigException(
                    "Ramp-up (" + request.rampUpSeconds() + "s) must be less than test duration ("
                            + request.durationSeconds() + "s)");
        }
    }

    private static void validateTypeSpecificFields(CreateTestRequest request) {
        TestType type = request.testType();

        switch (type) {
            case SPIKE -> {
                if (request.spikeUsers() == null) {
                    throw new InvalidTestConfigException("Spike test requires spikeUsers");
                }
                if (request.spikeUsers() <= 0) {
                    throw new InvalidTestConfigException("spikeUsers must be > 0");
                }
            }
            case STRESS, BREAKPOINT, SCALABILITY -> {
                if (request.stepSize() == null) {
                    throw new InvalidTestConfigException(type + " test requires stepSize");
                }
                if (request.stepSize() <= 0) {
                    throw new InvalidTestConfigException("stepSize must be > 0");
                }
                if (request.stepSize() > request.virtualUsers()) {
                    throw new InvalidTestConfigException(
                            "stepSize (" + request.stepSize() + ") cannot exceed virtualUsers ("
                                    + request.virtualUsers() + ")");
                }
            }
            default -> { /* LOAD, SOAK — no extra fields required */ }
        }
    }

    private static void validateBodyForMethod(CreateTestRequest request) {
        if (BODY_REQUIRED_METHODS.contains(request.httpMethod())
                && (request.requestBody() == null || request.requestBody().isBlank())) {
            throw new InvalidTestConfigException(
                    "Request body is required for " + request.httpMethod() + " requests");
        }
    }

}
