package com.stormapi.test.repository;

import com.stormapi.test.model.TestResult;
import com.stormapi.test.model.TestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
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

}
