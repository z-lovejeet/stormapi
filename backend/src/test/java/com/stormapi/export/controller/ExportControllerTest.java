package com.stormapi.export.controller;

import com.stormapi.auth.handler.OAuth2AuthenticationFailureHandler;
import com.stormapi.auth.handler.OAuth2AuthenticationSuccessHandler;
import com.stormapi.auth.jwt.JwtAuthenticationFilter;
import com.stormapi.auth.jwt.JwtTokenProvider;
import com.stormapi.auth.repository.AppUserRepository;
import com.stormapi.auth.service.CustomOAuth2UserService;
import com.stormapi.export.service.ExportService;
import com.stormapi.export.service.HtmlReportService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExportController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ExportController WebMvc Tests")
class ExportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExportService exportService;

    @MockitoBean
    private HtmlReportService htmlReportService;


    // Security infrastructure mocks
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private AppUserRepository appUserRepository;
    @MockitoBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean private CustomOAuth2UserService customOAuth2UserService;
    @MockitoBean private OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler;
    @MockitoBean private OAuth2AuthenticationFailureHandler oAuth2FailureHandler;

    @Test
    @DisplayName("GET /api/export/{resultId}/json returns JSON file download")
    void exportJson_returnsFile() throws Exception {
        doAnswer(invocation -> {
            java.io.OutputStream os = invocation.getArgument(1);
            os.write("{\"result\":{}}".getBytes());
            return null;
        }).when(exportService).exportJson(eq(1L), any());

        mockMvc.perform(get("/api/export/1/json"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"stormapi-result-1.json\""))
                .andExpect(content().contentType("application/json"));
    }

    @Test
    @DisplayName("GET /api/export/{resultId}/csv returns CSV file download")
    void exportCsv_returnsFile() throws Exception {
        doAnswer(invocation -> {
            java.io.OutputStream os = invocation.getArgument(1);
            os.write("timestamp,active_users\n2025-01-01T00:00:00Z,10\n".getBytes());
            return null;
        }).when(exportService).exportCsv(eq(1L), any());

        mockMvc.perform(get("/api/export/1/csv"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"stormapi-metrics-1.csv\""))
                .andExpect(content().contentType("text/csv"));
    }

}
