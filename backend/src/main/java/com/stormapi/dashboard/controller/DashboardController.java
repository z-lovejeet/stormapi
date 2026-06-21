package com.stormapi.dashboard.controller;

import com.stormapi.common.model.ApiResponse;
import com.stormapi.dashboard.dto.DashboardStatsResponse;
import com.stormapi.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for aggregated dashboard statistics.
 */
@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Aggregated statistics and overview")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/stats")
    @Operation(summary = "Get aggregated dashboard statistics")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getStats(
            HttpServletRequest httpRequest) {

        DashboardStatsResponse stats = dashboardService.getStats();
        return ResponseEntity.ok(ApiResponse.success(stats, httpRequest.getRequestURI()));
    }

}
