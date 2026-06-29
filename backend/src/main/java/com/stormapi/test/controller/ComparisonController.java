package com.stormapi.test.controller;

import com.stormapi.common.model.ApiResponse;
import com.stormapi.test.dto.ComparisonResponse;
import com.stormapi.test.service.ComparisonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for comparing two test results side-by-side.
 */
@RestController
@RequestMapping("/api/results")
@Tag(name = "Results", description = "Test result comparison")
public class ComparisonController {

    private final ComparisonService comparisonService;

    public ComparisonController(ComparisonService comparisonService) {
        this.comparisonService = comparisonService;
    }

    @GetMapping("/compare")
    @Operation(summary = "Compare two test results with computed deltas")
    public ResponseEntity<ApiResponse<ComparisonResponse>> compare(
            @RequestParam Long resultIdA,
            @RequestParam Long resultIdB,
            HttpServletRequest httpRequest) {

        if (resultIdA.equals(resultIdB)) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Bad Request",
                            "Cannot compare a result with itself",
                            "SAME_RESULT_COMPARISON", httpRequest.getRequestURI()));
        }

        ComparisonResponse comparison = comparisonService.compare(resultIdA, resultIdB);
        return ResponseEntity.ok(ApiResponse.success(comparison, httpRequest.getRequestURI()));
    }
}
