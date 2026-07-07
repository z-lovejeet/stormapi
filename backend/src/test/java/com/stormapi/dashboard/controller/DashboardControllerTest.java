package com.stormapi.dashboard.controller;

import com.stormapi.auth.handler.OAuth2AuthenticationFailureHandler;
import com.stormapi.auth.handler.OAuth2AuthenticationSuccessHandler;
import com.stormapi.auth.jwt.JwtAuthenticationFilter;
import com.stormapi.auth.jwt.JwtTokenProvider;
import com.stormapi.auth.repository.AppUserRepository;
import com.stormapi.auth.service.CustomOAuth2UserService;
import com.stormapi.dashboard.dto.DashboardStatsResponse;
import com.stormapi.dashboard.service.DashboardService;
import com.stormapi.test.model.TestType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("DashboardController WebMvc Tests")
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    // Security infrastructure mocks
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private AppUserRepository appUserRepository;
    @MockitoBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean private CustomOAuth2UserService customOAuth2UserService;
    @MockitoBean private OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler;
    @MockitoBean private OAuth2AuthenticationFailureHandler oAuth2FailureHandler;

    @Test
    @DisplayName("GET /api/dashboard/stats returns aggregated stats")
    void getStats_returnsAggregatedData() throws Exception {
        DashboardStatsResponse stats = new DashboardStatsResponse(
                10, 25, 2, 20, 3, 50000, 45.5, 100.0, 2.5,
                List.of(), Map.of(TestType.LOAD, 5L, TestType.STRESS, 3L)
        );
        when(dashboardService.getStats()).thenReturn(stats);

        mockMvc.perform(get("/api/dashboard/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalTests").value(10))
                .andExpect(jsonPath("$.data.totalRuns").value(25))
                .andExpect(jsonPath("$.data.runningTests").value(2))
                .andExpect(jsonPath("$.data.avgResponseTimeMs").value(45.5))
                .andExpect(jsonPath("$.data.testTypeDistribution.LOAD").value(5));
    }

}
