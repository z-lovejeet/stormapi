package com.stormapi.test.service;

import com.stormapi.common.exception.ResourceNotFoundException;
import com.stormapi.test.dto.ComparisonResponse;
import com.stormapi.test.dto.MetricDelta;
import com.stormapi.test.dto.TestResultResponse;
import com.stormapi.test.mapper.TestMapper;
import com.stormapi.test.model.TestResult;
import com.stormapi.test.repository.TestResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Computes percentage-change deltas between two test results for comparison view.
 */
@Service
public class ComparisonService {

    private final TestResultRepository resultRepository;

    public ComparisonService(TestResultRepository resultRepository) {
        this.resultRepository = resultRepository;
    }

    @Transactional(readOnly = true)
    public ComparisonResponse compare(Long resultIdA, Long resultIdB) {
        TestResult a = resultRepository.findById(resultIdA)
                .orElseThrow(() -> new ResourceNotFoundException("TestResult", resultIdA));
        TestResult b = resultRepository.findById(resultIdB)
                .orElseThrow(() -> new ResourceNotFoundException("TestResult", resultIdB));

        TestResultResponse respA = TestMapper.toResultResponse(a);
        TestResultResponse respB = TestMapper.toResultResponse(b);

        List<MetricDelta> deltas = new ArrayList<>();

        // Throughput: higher is better
        deltas.add(buildDelta("throughputRps", "Throughput (RPS)", a.getThroughputRps(), b.getThroughputRps(), true));
        deltas.add(buildDelta("totalRequests", "Total Requests", a.getTotalRequests(), b.getTotalRequests(), true));
        deltas.add(buildDelta("successCount", "Successful Requests", a.getSuccessCount(), b.getSuccessCount(), true));

        // Latency: lower is better
        deltas.add(buildDelta("avgResponseTimeMs", "Avg Response Time", a.getAvgResponseTimeMs(), b.getAvgResponseTimeMs(), false));
        deltas.add(buildDelta("p50Ms", "P50 Latency", a.getP50Ms(), b.getP50Ms(), false));
        deltas.add(buildDelta("p75Ms", "P75 Latency", a.getP75Ms(), b.getP75Ms(), false));
        deltas.add(buildDelta("p90Ms", "P90 Latency", a.getP90Ms(), b.getP90Ms(), false));
        deltas.add(buildDelta("p95Ms", "P95 Latency", a.getP95Ms(), b.getP95Ms(), false));
        deltas.add(buildDelta("p99Ms", "P99 Latency", a.getP99Ms(), b.getP99Ms(), false));

        // Error: lower is better
        deltas.add(buildDelta("errorRate", "Error Rate", a.getErrorRate(), b.getErrorRate(), false));
        deltas.add(buildDelta("failureCount", "Failed Requests", a.getFailureCount(), b.getFailureCount(), false));

        return new ComparisonResponse(respA, respB, deltas);
    }

    /**
     * Build a MetricDelta.
     * @param higherIsBetter if true, an increase from A to B is "improved"
     */
    private MetricDelta buildDelta(String field, String label, double valA, double valB, boolean higherIsBetter) {
        double delta = valB - valA;
        double deltaPercent = (valA == 0) ? (valB == 0 ? 0.0 : 100.0) : (delta / valA) * 100.0;
        boolean improved = higherIsBetter ? delta > 0 : delta < 0;
        return new MetricDelta(field, label, valA, valB, delta, deltaPercent, improved);
    }
}
