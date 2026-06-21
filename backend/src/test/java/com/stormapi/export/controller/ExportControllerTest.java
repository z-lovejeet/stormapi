package com.stormapi.export.controller;

import com.stormapi.export.service.ExportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExportController.class)
@DisplayName("ExportController WebMvc Tests")
class ExportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExportService exportService;

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
