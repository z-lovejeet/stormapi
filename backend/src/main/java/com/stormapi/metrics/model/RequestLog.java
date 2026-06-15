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
 * Individual HTTP request/response record for request-level drill-down.
 *
 * WARNING: This table can grow extremely large (10,000+ rows per test run).
 * All queries MUST be paginated. Batch inserts (saveAll in chunks of 100) are required.
 */
@Entity
@Table(name = "request_logs", indexes = {
        @Index(name = "idx_request_log_result_id", columnList = "test_result_id"),
        @Index(name = "idx_request_log_success", columnList = "test_result_id, success")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestLog extends BaseEntity {

    /** The test result this log entry belongs to */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_result_id", nullable = false)
    private TestResult testResult;

    /** When the request was sent */
    @Column(nullable = false)
    private Instant timestamp;

    /** Target URL */
    @Column(nullable = false, length = 2048)
    private String url;

    /** HTTP method used */
    @Column(nullable = false, length = 10)
    private String method;

    /** HTTP response status code */
    @Column(nullable = false)
    private int statusCode;

    /** Response time in milliseconds */
    @Column(nullable = false)
    private long responseTimeMs;

    /** Response body size in bytes */
    @Column(nullable = false)
    private long responseSize;

    /** Error description if the request failed */
    private String errorMessage;

    /** True if the response was 2xx */
    @Column(nullable = false)
    private boolean success;

}
