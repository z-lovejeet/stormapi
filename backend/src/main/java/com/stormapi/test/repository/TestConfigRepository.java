package com.stormapi.test.repository;

import com.stormapi.test.model.TestConfig;
import com.stormapi.test.model.TestStatus;
import com.stormapi.test.model.TestType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for TestConfig entity.
 */
@Repository
public interface TestConfigRepository extends JpaRepository<TestConfig, Long> {

    List<TestConfig> findByStatusIn(List<TestStatus> statuses);

    List<TestConfig> findByTestType(TestType type);

    List<TestConfig> findAllByOrderByCreatedAtDesc();

    long countByStatus(TestStatus status);

    // Paginated queries for Phase 8 REST API
    Page<TestConfig> findByStatus(TestStatus status, Pageable pageable);

    Page<TestConfig> findByTestType(TestType type, Pageable pageable);

    Page<TestConfig> findByStatusAndTestType(TestStatus status, TestType type, Pageable pageable);

    List<TestConfig> findTop5ByOrderByCreatedAtDesc();

    long countByTestType(TestType type);

}
