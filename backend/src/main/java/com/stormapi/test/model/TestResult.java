package com.stormapi.test.model;

import com.stormapi.common.model.BaseEntity;
import com.stormapi.metrics.model.MetricSnapshot;
import com.stormapi.metrics.model.RequestLog;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Stores aggregated results of one test execution.
 * Includes latency percentiles, throughput, error rate, and timing information.
 * One TestConfig can produce many TestResults (re-runs).
 */
@Entity
@Table(name = "test_results", indexes = {
        @Index(name = "idx_test_result_config_id", columnList = "test_config_id"),
        @Index(name = "idx_test_result_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestResult extends BaseEntity {

    /** The test configuration this result belongs to */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_config_id", nullable = false)
    private TestConfig testConfig;

    /** Execution status */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TestStatus status;

    // ── Aggregate Counts ──

    /** Total HTTP requests sent */
    @Column(nullable = false)
    @Builder.Default
    private long totalRequests = 0;

    /** Successful requests (2xx responses) */
    @Column(nullable = false)
    @Builder.Default
    private long successCount = 0;

    /** Failed requests (non-2xx + errors) */
    @Column(nullable = false)
    @Builder.Default
    private long failureCount = 0;

    // ── Latency Metrics (milliseconds) ──

    @Column(nullable = false)
    @Builder.Default
    private double avgResponseTimeMs = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private double minResponseTimeMs = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private double maxResponseTimeMs = 0.0;

    /** 50th percentile (median) */
    @Column(nullable = false)
    @Builder.Default
    private double p50Ms = 0.0;

    /** 75th percentile */
    @Column(nullable = false)
    @Builder.Default
    private double p75Ms = 0.0;

    /** 90th percentile */
    @Column(nullable = false)
    @Builder.Default
    private double p90Ms = 0.0;

    /** 95th percentile */
    @Column(nullable = false)
    @Builder.Default
    private double p95Ms = 0.0;

    /** 99th percentile */
    @Column(nullable = false)
    @Builder.Default
    private double p99Ms = 0.0;

    // ── Throughput & Error Metrics ──

    /** Requests per second */
    @Column(nullable = false)
    @Builder.Default
    private double throughputRps = 0.0;

    /** Error rate percentage (0.0–100.0) */
    @Column(nullable = false)
    @Builder.Default
    private double errorRate = 0.0;

    /** Total response data received in bytes */
    @Column(nullable = false)
    @Builder.Default
    private long totalDataBytes = 0;

    // ── Timing ──

    /** When the test started executing */
    @Column(nullable = false)
    private Instant startedAt;

    /** When the test finished (null if still running) */
    private Instant completedAt;

    /** Actual test duration in milliseconds */
    @Column(nullable = false)
    @Builder.Default
    private long durationMs = 0;

    /** Users at breaking point — populated only for BREAKPOINT test type */
    private Integer breakpointUsers;

    // ── Relationships ──

    /** Time-series metric snapshots (1 per second during test) */
    @OneToMany(mappedBy = "testResult", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MetricSnapshot> metricSnapshots = new ArrayList<>();

    /** Individual request log entries */
    @OneToMany(mappedBy = "testResult", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RequestLog> requestLogs = new ArrayList<>();

}
