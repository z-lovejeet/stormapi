package com.stormapi.export.controller;

import com.stormapi.export.service.ExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * REST controller for exporting test results as downloadable files.
 */
@RestController
@RequestMapping("/api/export")
@Tag(name = "Export", description = "Export test results as JSON or CSV")
public class ExportController {

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping("/{resultId}/json")
    @Operation(summary = "Download full test result as JSON file")
    public ResponseEntity<StreamingResponseBody> exportJson(@PathVariable Long resultId) {
        StreamingResponseBody body = outputStream -> exportService.exportJson(resultId, outputStream);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"stormapi-result-" + resultId + ".json\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    @GetMapping("/{resultId}/csv")
    @Operation(summary = "Download metric snapshots as CSV file")
    public ResponseEntity<StreamingResponseBody> exportCsv(@PathVariable Long resultId) {
        StreamingResponseBody body = outputStream -> exportService.exportCsv(resultId, outputStream);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"stormapi-metrics-" + resultId + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(body);
    }

}
