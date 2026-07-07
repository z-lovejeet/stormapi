package com.stormapi.common.exception;

import com.stormapi.auth.handler.OAuth2AuthenticationFailureHandler;
import com.stormapi.auth.handler.OAuth2AuthenticationSuccessHandler;
import com.stormapi.auth.jwt.JwtAuthenticationFilter;
import com.stormapi.auth.jwt.JwtTokenProvider;
import com.stormapi.auth.repository.AppUserRepository;
import com.stormapi.auth.service.CustomOAuth2UserService;
import com.stormapi.common.model.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for GlobalExceptionHandler.
 * Verifies that all exception types return structured JSON with correct HTTP status codes.
 */
@WebMvcTest(FakeExceptionController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    // Security infrastructure mocks
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private AppUserRepository appUserRepository;
    @MockitoBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean private CustomOAuth2UserService customOAuth2UserService;
    @MockitoBean private OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler;
    @MockitoBean private OAuth2AuthenticationFailureHandler oAuth2FailureHandler;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public FakeExceptionController fakeExceptionController() {
            return new FakeExceptionController();
        }
    }

    @Test
    void resourceNotFound_returns404WithStructuredJson() throws Exception {
        mockMvc.perform(get("/test/not-found").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.status").value(404))
                .andExpect(jsonPath("$.error.errorCode").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("TestConfig with id 999 not found"))
                .andExpect(jsonPath("$.path").value("/test/not-found"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void testAlreadyRunning_returns409() throws Exception {
        mockMvc.perform(get("/test/already-running").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.status").value(409))
                .andExpect(jsonPath("$.error.errorCode").value("TEST_ALREADY_RUNNING"));
    }

    @Test
    void invalidTestConfig_returns422() throws Exception {
        mockMvc.perform(get("/test/invalid-config").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.status").value(422))
                .andExpect(jsonPath("$.error.errorCode").value("INVALID_TEST_CONFIG"));
    }

    @Test
    void unexpectedException_returns500WithoutStackTrace() throws Exception {
        mockMvc.perform(get("/test/unexpected").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.status").value(500))
                .andExpect(jsonPath("$.error.errorCode").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.error.message").value("An unexpected error occurred. Please try again later."));
    }

    @Test
    void methodNotAllowed_returns405() throws Exception {
        mockMvc.perform(put("/test/not-found").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.status").value(405))
                .andExpect(jsonPath("$.error.errorCode").value("METHOD_NOT_ALLOWED"));
    }

}

/**
 * Standalone controller that throws exceptions for testing GlobalExceptionHandler.
 * Registered as a bean via @TestConfiguration.
 */
@RestController
class FakeExceptionController {

    @GetMapping("/test/not-found")
    public ApiResponse<String> notFound() {
        throw new ResourceNotFoundException("TestConfig", 999L);
    }

    @GetMapping("/test/already-running")
    public ApiResponse<String> alreadyRunning() {
        throw new TestAlreadyRunningException(42L);
    }

    @GetMapping("/test/invalid-config")
    public ApiResponse<String> invalidConfig() {
        throw new InvalidTestConfigException("rampUpSeconds cannot exceed durationSeconds");
    }

    @GetMapping("/test/unexpected")
    public ApiResponse<String> unexpected() {
        throw new RuntimeException("Something broke internally");
    }
}
