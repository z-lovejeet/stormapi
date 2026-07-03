package com.stormapi.export.controller;

import com.stormapi.export.service.ExportService;
import com.stormapi.export.service.HtmlReportService;
import com.stormapi.export.service.PdfReportService;
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
 * Supports JSON, CSV, self-contained HTML, and PDF report formats.
 */
@RestController
@RequestMapping("/api/export")
@Tag(name = "Export", description = "Export test results as JSON, CSV, HTML, or PDF report")
public class ExportController {

    private final ExportService exportService;
    private final HtmlReportService htmlReportService;
    private final PdfReportService pdfReportService;

    public ExportController(ExportService exportService,
                            HtmlReportService htmlReportService,
                            PdfReportService pdfReportService) {
        this.exportService = exportService;
        this.htmlReportService = htmlReportService;
        this.pdfReportService = pdfReportService;
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

    @GetMapping("/{resultId}/html")
    @Operation(summary = "Download self-contained HTML test report")
    public ResponseEntity<StreamingResponseBody> exportHtml(@PathVariable Long resultId) {
        StreamingResponseBody body = outputStream -> htmlReportService.exportHtml(resultId, outputStream);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"stormapi-report-" + resultId + ".html\"")
                .contentType(MediaType.TEXT_HTML)
                .body(body);
    }

    @GetMapping("/{resultId}/pdf")
    @Operation(summary = "Download PDF test report")
    public ResponseEntity<StreamingResponseBody> exportPdf(@PathVariable Long resultId) {
        StreamingResponseBody body = outputStream -> pdfReportService.exportPdf(resultId, outputStream);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"stormapi-report-" + resultId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(body);
    }
}
