package com.stormapi.metrics.model;

import com.stormapi.common.model.BaseEntity;
import com.stormapi.test.model.TestResult;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * One-per-second time-series data point captured during test execution.
 * Powers the live monitoring charts and post-test timeline visualization.
 *
 * A 5-minute test produces ~300 MetricSnapshot rows.
 */
@Entity
@Table(name = "metric_snapshots", indexes = {
        @Index(name = "idx_metric_snapshot_result_ts", columnList = "test_result_id, timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetricSnapshot extends BaseEntity {

    /** The test result this snapshot belongs to */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_result_id", nullable = false)
    private TestResult testResult;

    /** The second this snapshot represents */
    @Column(nullable = false)
    private Instant timestamp;

    /** Virtual users active at this second */
    @Column(nullable = false)
    private int activeUsers;

    /** Requests per second at this point */
    @Column(nullable = false)
    private double requestsPerSecond;

    /** Average response time this second */
    @Column(nullable = false)
    private double avgResponseTimeMs;

    /** Error rate percentage this second */
    @Column(nullable = false)
    private double errorRate;

    /** 95th percentile latency this second */
    @Column(nullable = false)
    private double p95Ms;

    /** Running total of all requests sent so far */
    @Column(nullable = false)
    private long cumulativeRequests;

    /** Running total of all errors so far */
    @Column(nullable = false)
    private long cumulativeErrors;

}
