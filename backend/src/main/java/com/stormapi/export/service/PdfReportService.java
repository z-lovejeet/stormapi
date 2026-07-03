package com.stormapi.export.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Generates PDF reports by delegating HTML rendering to HtmlReportService,
 * then converting the HTML string to PDF via openhtmltopdf.
 * Reuses the same report-template.html — zero template duplication.
 */
@Service
public class PdfReportService {

    private final HtmlReportService htmlReportService;

    public PdfReportService(HtmlReportService htmlReportService) {
        this.htmlReportService = htmlReportService;
    }

    /**
     * Renders the HTML report and converts it to PDF, writing directly to the output stream.
     */
    public void exportPdf(Long resultId, OutputStream outputStream) throws IOException {
        // Step 1: Render Thymeleaf template to HTML string (reuses HtmlReportService)
        String htmlContent = htmlReportService.renderHtmlString(resultId);

        // Step 2: Convert HTML string to PDF via openhtmltopdf
        try {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(htmlContent, null);
            builder.toStream(outputStream);
            builder.run();
        } catch (Exception e) {
            throw new IOException("Failed to generate PDF report", e);
        }
    }
}
