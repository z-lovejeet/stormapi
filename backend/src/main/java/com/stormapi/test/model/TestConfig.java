package com.stormapi.test.model;

import com.stormapi.common.converter.MapToJsonConverter;
import com.stormapi.common.model.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Master configuration entity — stores everything needed to run a performance test.
 * A single TestConfig can be re-run multiple times, producing multiple TestResults.
 */
@Entity
@Table(name = "test_configs", indexes = {
        @Index(name = "idx_test_config_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestConfig extends BaseEntity {

    /** User-given name for the test */
    @Column(nullable = false, length = 255)
    private String name;

    /** Optional description */
    @Column(length = 1000)
    private String description;

    /** Target API endpoint URL */
    @Column(nullable = false, length = 2048)
    private String targetUrl;

    /** HTTP method to use */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HttpMethod httpMethod;

    /** Request headers stored as JSON */
    @Convert(converter = MapToJsonConverter.class)
    @Column(columnDefinition = "TEXT")
    private Map<String, String> headers;

    /** Request body (JSON, XML, etc.) */
    @Column(columnDefinition = "TEXT")
    private String requestBody;

    /** Type of performance test */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TestType testType;

    /** Number of concurrent virtual users */
    @Column(nullable = false)
    private int virtualUsers;

    /** Test duration in seconds */
    @Column(nullable = false)
    private int durationSeconds;

    /** Ramp-up period in seconds (0 = all users start instantly) */
    @Column(nullable = false)
    @Builder.Default
    private int rampUpSeconds = 0;

    /** Users per step — for STRESS, BREAKPOINT, SCALABILITY test types */
    private Integer stepSize;

    /** Duration per step in seconds */
    private Integer stepDurationSeconds;

    /** Spike user count — for SPIKE test type only */
    private Integer spikeUsers;

    /** Number of retries on request failure */
    @Column(nullable = false)
    @Builder.Default
    private int maxRetries = 0;

    /** Request timeout in milliseconds */
    @Column(nullable = false)
    @Builder.Default
    private int timeoutMs = 5000;

    /** Pause between requests per virtual user in milliseconds */
    @Column(nullable = false)
    @Builder.Default
    private int thinkTimeMs = 0;

    /** Current lifecycle state */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TestStatus status = TestStatus.CREATED;

    /** All execution results for this config (one per run) */
    @OneToMany(mappedBy = "testConfig", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TestResult> results = new ArrayList<>();

}
