package com.stormapi.test.repository;

import com.stormapi.test.model.TestResult;
import com.stormapi.test.model.TestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for TestResult entity.
 */
@Repository
public interface TestResultRepository extends JpaRepository<TestResult, Long> {

    List<TestResult> findByTestConfigIdOrderByCreatedAtDesc(Long configId);

    Optional<TestResult> findTopByTestConfigIdOrderByCreatedAtDesc(Long configId);

    List<TestResult> findByStatus(TestStatus status);

    // Paginated queries for Phase 8 REST API
    Page<TestResult> findByTestConfigId(Long configId, Pageable pageable);

    // Aggregate queries for Dashboard
    @Query("SELECT COALESCE(SUM(r.totalRequests), 0) FROM TestResult r")
    long sumTotalRequests();

    @Query("SELECT COALESCE(AVG(r.avgResponseTimeMs), 0) FROM TestResult r WHERE r.status = 'COMPLETED'")
    double avgResponseTimeMs();

    @Query("SELECT COALESCE(AVG(r.throughputRps), 0) FROM TestResult r WHERE r.status = 'COMPLETED'")
    double avgThroughputRps();

    @Query("SELECT COALESCE(AVG(r.errorRate), 0) FROM TestResult r WHERE r.status = 'COMPLETED'")
    double avgErrorRate();

}
