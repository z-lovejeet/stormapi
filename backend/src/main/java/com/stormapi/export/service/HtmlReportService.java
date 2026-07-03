package com.stormapi.export.service;

import com.stormapi.common.exception.ResourceNotFoundException;
import com.stormapi.metrics.model.MetricSnapshot;
import com.stormapi.metrics.repository.MetricSnapshotRepository;
import com.stormapi.test.dto.TestResultResponse;
import com.stormapi.test.mapper.TestMapper;
import com.stormapi.test.model.TestConfig;
import com.stormapi.test.model.TestResult;
import com.stormapi.test.repository.TestResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Generates self-contained HTML reports using Thymeleaf template processing.
 * The rendered HTML includes all CSS inline — no external resources needed.
 */
@Service
@Transactional(readOnly = true)
public class HtmlReportService {

    private final TestResultRepository testResultRepository;
    private final MetricSnapshotRepository metricSnapshotRepository;
    private final TemplateEngine templateEngine;

    private static final DateTimeFormatter DATETIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault());

    public HtmlReportService(TestResultRepository testResultRepository,
                             MetricSnapshotRepository metricSnapshotRepository,
                             TemplateEngine templateEngine) {
        this.testResultRepository = testResultRepository;
        this.metricSnapshotRepository = metricSnapshotRepository;
        this.templateEngine = templateEngine;
    }

    /**
     * Renders a self-contained HTML report and writes it to the output stream.
     */
    public void exportHtml(Long resultId, OutputStream outputStream) throws IOException {
        TestResult result = testResultRepository.findById(resultId)
                .orElseThrow(() -> new ResourceNotFoundException("TestResult", resultId));

        TestResultResponse resultDto = TestMapper.toResultResponse(result);
        TestConfig config = result.getTestConfig();

        List<MetricSnapshot> snapshots = metricSnapshotRepository
                .findByTestResultIdOrderByTimestampAsc(resultId);

        // Build template context
        Context ctx = buildContext(resultDto, config, snapshots);

        // Render template to output stream
        OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
        templateEngine.process("report-template", ctx, writer);
        writer.flush();
    }

    /**
     * Renders the HTML report template to a String (used by PdfReportService).
     */
    public String renderHtmlString(Long resultId) {
        TestResult result = testResultRepository.findById(resultId)
                .orElseThrow(() -> new ResourceNotFoundException("TestResult", resultId));

        TestResultResponse resultDto = TestMapper.toResultResponse(result);
        TestConfig config = result.getTestConfig();

        List<MetricSnapshot> snapshots = metricSnapshotRepository
                .findByTestResultIdOrderByTimestampAsc(resultId);

        Context ctx = buildContext(resultDto, config, snapshots);
        return templateEngine.process("report-template", ctx);
    }

    /**
     * Builds the Thymeleaf context with all template variables.
     */
    private Context buildContext(TestResultResponse resultDto, TestConfig config,
                                 List<MetricSnapshot> snapshots) {
        Context ctx = new Context(Locale.ENGLISH);

        // Test config metadata
        ctx.setVariable("testName", config.getName());
        ctx.setVariable("targetUrl", config.getTargetUrl());
        ctx.setVariable("httpMethod", config.getHttpMethod().name());
        ctx.setVariable("testType", config.getTestType().name());
        ctx.setVariable("virtualUsers", config.getVirtualUsers());
        ctx.setVariable("durationSeconds", config.getDurationSeconds());

        // Result data
        ctx.setVariable("result", resultDto);
        ctx.setVariable("status", resultDto.status().name());
        ctx.setVariable("passed", resultDto.errorRate() < 5.0);

        // Formatted dates
        if (resultDto.startedAt() != null) {
            ctx.setVariable("startedAt", DATETIME_FMT.format(resultDto.startedAt()));
        }
        if (resultDto.completedAt() != null) {
            ctx.setVariable("completedAt", DATETIME_FMT.format(resultDto.completedAt()));
        }

        // Duration formatted
        long durationSec = resultDto.durationMs() / 1000;
        ctx.setVariable("durationFormatted",
                String.format("%dm %ds", durationSec / 60, durationSec % 60));

        // Success rate
        double successRate = resultDto.totalRequests() > 0
                ? (resultDto.successCount() * 100.0 / resultDto.totalRequests())
                : 0.0;
        ctx.setVariable("successRate", String.format("%.1f", successRate));

        // Timeline data for the snapshots table
        ctx.setVariable("snapshots", snapshots.stream()
                .map(s -> Map.of(
                        "timestamp", s.getTimestamp().toString(),
                        "activeUsers", s.getActiveUsers(),
                        "rps", String.format("%.1f", s.getRequestsPerSecond()),
                        "avgMs", String.format("%.1f", s.getAvgResponseTimeMs()),
                        "errorRate", String.format("%.1f", s.getErrorRate()),
                        "p95Ms", String.format("%.1f", s.getP95Ms())
                ))
                .collect(Collectors.toList()));

        ctx.setVariable("snapshotCount", snapshots.size());

        // SVG chart data points (normalize for 600px wide, 200px tall SVG)
        if (!snapshots.isEmpty()) {
            double maxMs = snapshots.stream()
                    .mapToDouble(MetricSnapshot::getAvgResponseTimeMs).max().orElse(1.0);
            double maxRps = snapshots.stream()
                    .mapToDouble(MetricSnapshot::getRequestsPerSecond).max().orElse(1.0);

            int width = 600;
            int height = 200;
            double xStep = snapshots.size() > 1
                    ? (double) width / (snapshots.size() - 1) : width;

            // Build SVG polyline points string for response time and throughput
            StringBuilder rtPoints = new StringBuilder();
            StringBuilder rpsPoints = new StringBuilder();
            for (int i = 0; i < snapshots.size(); i++) {
                double x = i * xStep;
                double yRt = height - (snapshots.get(i).getAvgResponseTimeMs()
                        / Math.max(maxMs, 0.001)) * height;
                double yRps = height - (snapshots.get(i).getRequestsPerSecond()
                        / Math.max(maxRps, 0.001)) * height;
                rtPoints.append(String.format("%.1f,%.1f ", x, yRt));
                rpsPoints.append(String.format("%.1f,%.1f ", x, yRps));
            }
            ctx.setVariable("rtChartPoints", rtPoints.toString().trim());
            ctx.setVariable("rpsChartPoints", rpsPoints.toString().trim());
            ctx.setVariable("maxMs", String.format("%.0f", maxMs));
            ctx.setVariable("maxRps", String.format("%.0f", maxRps));
        }

        // Generate report date
        ctx.setVariable("generatedAt", DATETIME_FMT.format(Instant.now()));

        return ctx;
    }
}
