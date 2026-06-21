package com.stormapi.test.service;

import com.stormapi.common.exception.ResourceNotFoundException;
import com.stormapi.test.dto.TestSummaryResponse;
import com.stormapi.test.mapper.TestMapper;
import com.stormapi.test.model.TestConfig;
import com.stormapi.test.model.TestResult;
import com.stormapi.test.model.TestStatus;
import com.stormapi.test.model.TestType;
import com.stormapi.test.repository.TestConfigRepository;
import com.stormapi.test.repository.TestResultRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only query service for tests — CQRS-lite separation from TestOrchestrator (commands).
 * All methods are {@code @Transactional(readOnly = true)} for Hibernate read optimizations.
 */
@Service
@Transactional(readOnly = true)
public class TestQueryService {

    private final TestConfigRepository testConfigRepository;
    private final TestResultRepository testResultRepository;

    public TestQueryService(TestConfigRepository testConfigRepository,
                            TestResultRepository testResultRepository) {
        this.testConfigRepository = testConfigRepository;
        this.testResultRepository = testResultRepository;
    }

    /**
     * Get a single test config by ID.
     * @throws ResourceNotFoundException if not found
     */
    public TestConfig getTestConfig(Long id) {
        return testConfigRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TestConfig", id));
    }

    /**
     * Paginated, filterable test config list.
     * Filters by status and/or type if provided; returns all if both null.
     */
    public Page<TestConfig> listTests(TestStatus status, TestType type, Pageable pageable) {
        if (status != null && type != null) {
            return testConfigRepository.findByStatusAndTestType(status, type, pageable);
        } else if (status != null) {
            return testConfigRepository.findByStatus(status, pageable);
        } else if (type != null) {
            return testConfigRepository.findByTestType(type, pageable);
        }
        return testConfigRepository.findAll(pageable);
    }

    /**
     * Get the most recent execution result for a test config.
     * @throws ResourceNotFoundException if config or result not found
     */
    public TestResult getLatestResult(Long configId) {
        // Verify config exists first
        if (!testConfigRepository.existsById(configId)) {
            throw new ResourceNotFoundException("TestConfig", configId);
        }
        return testResultRepository.findTopByTestConfigIdOrderByCreatedAtDesc(configId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No results found for TestConfig " + configId));
    }

    /**
     * Paginated results list for a test config.
     * @throws ResourceNotFoundException if config not found
     */
    public Page<TestResult> getResults(Long configId, Pageable pageable) {
        if (!testConfigRepository.existsById(configId)) {
            throw new ResourceNotFoundException("TestConfig", configId);
        }
        return testResultRepository.findByTestConfigId(configId, pageable);
    }

    /**
     * Enriches a page of TestConfigs with latest result metrics for summary responses.
     * Uses one query per item (bounded by page size, typically 20).
     */
    public Page<TestSummaryResponse> toSummaryPage(Page<TestConfig> configs) {
        return configs.map(config -> {
            TestResult latest = testResultRepository
                    .findTopByTestConfigIdOrderByCreatedAtDesc(config.getId())
                    .orElse(null);
            return TestMapper.toSummary(config, latest);
        });
    }

}
