package com.stormapi.test.validation;

import com.stormapi.common.exception.InvalidTestConfigException;
import com.stormapi.test.dto.CreateTestRequest;
import com.stormapi.test.model.HttpMethod;
import com.stormapi.test.model.TestType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TestConfigValidator Unit Tests")
class TestConfigValidatorTest {

    @Test
    @DisplayName("Valid LOAD test passes validation")
    void validLoadTest_passes() {
        CreateTestRequest request = buildRequest(TestType.LOAD, HttpMethod.GET,
                null, null, null, 0, null);
        assertDoesNotThrow(() -> TestConfigValidator.validate(request));
    }

    @Test
    @DisplayName("Ramp-up exceeding duration throws")
    void rampUpExceedsDuration_throws() {
        CreateTestRequest request = buildRequest(TestType.LOAD, HttpMethod.GET,
                null, null, null, 60, null); // rampUp=60, duration=60
        assertThrows(InvalidTestConfigException.class,
                () -> TestConfigValidator.validate(request));
    }

    @Test
    @DisplayName("SPIKE without spikeUsers throws")
    void spikeWithoutSpikeUsers_throws() {
        CreateTestRequest request = buildRequest(TestType.SPIKE, HttpMethod.GET,
                null, null, null, 0, null);
        InvalidTestConfigException ex = assertThrows(InvalidTestConfigException.class,
                () -> TestConfigValidator.validate(request));
        assertTrue(ex.getMessage().contains("spikeUsers"));
    }

    @Test
    @DisplayName("SPIKE with zero spikeUsers throws")
    void spikeWithZeroSpikeUsers_throws() {
        CreateTestRequest request = buildRequest(TestType.SPIKE, HttpMethod.GET,
                null, null, 0, 0, null);
        assertThrows(InvalidTestConfigException.class,
                () -> TestConfigValidator.validate(request));
    }

    @Test
    @DisplayName("STRESS without stepSize throws")
    void stressWithoutStepSize_throws() {
        CreateTestRequest request = buildRequest(TestType.STRESS, HttpMethod.GET,
                null, null, null, 0, null);
        assertThrows(InvalidTestConfigException.class,
                () -> TestConfigValidator.validate(request));
    }

    @Test
    @DisplayName("BREAKPOINT without stepSize throws")
    void breakpointWithoutStepSize_throws() {
        CreateTestRequest request = buildRequest(TestType.BREAKPOINT, HttpMethod.GET,
                null, null, null, 0, null);
        assertThrows(InvalidTestConfigException.class,
                () -> TestConfigValidator.validate(request));
    }

    @Test
    @DisplayName("SCALABILITY without stepSize throws")
    void scalabilityWithoutStepSize_throws() {
        CreateTestRequest request = buildRequest(TestType.SCALABILITY, HttpMethod.GET,
                null, null, null, 0, null);
        assertThrows(InvalidTestConfigException.class,
                () -> TestConfigValidator.validate(request));
    }

    @Test
    @DisplayName("stepSize exceeding virtualUsers throws")
    void stepSizeExceedsUsers_throws() {
        CreateTestRequest request = buildRequest(TestType.STRESS, HttpMethod.GET,
                200, null, null, 0, null); // stepSize=200, virtualUsers=100
        assertThrows(InvalidTestConfigException.class,
                () -> TestConfigValidator.validate(request));
    }

    @Test
    @DisplayName("POST without body throws")
    void postWithoutBody_throws() {
        CreateTestRequest request = buildRequest(TestType.LOAD, HttpMethod.POST,
                null, null, null, 0, null);
        assertThrows(InvalidTestConfigException.class,
                () -> TestConfigValidator.validate(request));
    }

    @Test
    @DisplayName("LOAD test with optional fields passes")
    void loadTestWithOptionalFields_passes() {
        CreateTestRequest request = buildRequest(TestType.LOAD, HttpMethod.POST,
                10, 5, null, 5, "{\"data\":true}");
        assertDoesNotThrow(() -> TestConfigValidator.validate(request));
    }

    private CreateTestRequest buildRequest(TestType type, HttpMethod method,
                                           Integer stepSize, Integer stepDurationSeconds,
                                           Integer spikeUsers, int rampUpSeconds,
                                           String body) {
        return new CreateTestRequest(
                "Test", null, "https://api.example.com", method,
                null, body, type, 100, 60, rampUpSeconds,
                stepSize, stepDurationSeconds, spikeUsers,
                0, 5000, 0, false
        );
    }

}
