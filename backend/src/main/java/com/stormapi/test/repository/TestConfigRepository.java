package com.stormapi.test.repository;

import com.stormapi.test.model.TestConfig;
import com.stormapi.test.model.TestStatus;
import com.stormapi.test.model.TestType;
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

}
