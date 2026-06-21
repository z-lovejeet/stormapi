package com.stormapi.export.service;

import com.stormapi.common.exception.ResourceNotFoundException;
import com.stormapi.metrics.model.MetricSnapshot;
import com.stormapi.metrics.repository.MetricSnapshotRepository;
import com.stormapi.test.dto.TestResultResponse;
import com.stormapi.test.mapper.TestMapper;
import com.stormapi.test.model.TestResult;
import com.stormapi.test.repository.TestResultRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for exporting test results as JSON or CSV files.
 * CSV uses streaming writes to handle large datasets without buffering.
 */
@Service
@Transactional(readOnly = true)
public class ExportService {

    private final TestResultRepository testResultRepository;
    private final MetricSnapshotRepository metricSnapshotRepository;
    private final ObjectMapper objectMapper;

    public ExportService(TestResultRepository testResultRepository,
                         MetricSnapshotRepository metricSnapshotRepository,
                         ObjectMapper objectMapper) {
        this.testResultRepository = testResultRepository;
        this.metricSnapshotRepository = metricSnapshotRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Exports full test result + metric snapshots as structured JSON.
     */
    public void exportJson(Long resultId, OutputStream outputStream) throws IOException {
        TestResult result = getResult(resultId);
        TestResultResponse resultDto = TestMapper.toResultResponse(result);
        List<MetricSnapshot> snapshots = metricSnapshotRepository
                .findByTestResultIdOrderByTimestampAsc(resultId);

        Map<String, Object> export = new LinkedHashMap<>();
        export.put("result", resultDto);
        export.put("metricSnapshots", snapshots.stream()
                .map(s -> Map.of(
                        "timestamp", s.getTimestamp().toString(),
                        "activeUsers", s.getActiveUsers(),
                        "requestsPerSecond", s.getRequestsPerSecond(),
                        "avgResponseTimeMs", s.getAvgResponseTimeMs(),
                        "errorRate", s.getErrorRate(),
                        "p95Ms", s.getP95Ms(),
                        "cumulativeRequests", s.getCumulativeRequests(),
                        "cumulativeErrors", s.getCumulativeErrors()
                ))
                .toList());

        objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputStream, export);
    }

    /**
     * Exports metric snapshots as CSV with streaming writes.
     * Writes directly to OutputStream — no intermediate buffering.
     */
    public void exportCsv(Long resultId, OutputStream outputStream) {
        getResult(resultId); // verify exists
        List<MetricSnapshot> snapshots = metricSnapshotRepository
                .findByTestResultIdOrderByTimestampAsc(resultId);

        PrintWriter writer = new PrintWriter(outputStream);
        // Header row
        writer.println("timestamp,active_users,requests_per_second,avg_response_time_ms,error_rate,p95_ms,cumulative_requests,cumulative_errors");

        // Data rows — stream directly
        for (MetricSnapshot s : snapshots) {
            writer.printf("%s,%d,%.2f,%.2f,%.2f,%.2f,%d,%d%n",
                    s.getTimestamp(),
                    s.getActiveUsers(),
                    s.getRequestsPerSecond(),
                    s.getAvgResponseTimeMs(),
                    s.getErrorRate(),
                    s.getP95Ms(),
                    s.getCumulativeRequests(),
                    s.getCumulativeErrors());
        }
        writer.flush();
    }

    private TestResult getResult(Long resultId) {
        return testResultRepository.findById(resultId)
                .orElseThrow(() -> new ResourceNotFoundException("TestResult", resultId));
    }

}
