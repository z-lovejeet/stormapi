package com.stormapi.metrics.repository;

import com.stormapi.metrics.model.MetricSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Spring Data JPA repository for MetricSnapshot entity.
 * Provides time-series data access for charts and post-test analysis.
 */
@Repository
public interface MetricSnapshotRepository extends JpaRepository<MetricSnapshot, Long> {

    List<MetricSnapshot> findByTestResultIdOrderByTimestampAsc(Long resultId);

    @Modifying
    @Transactional
    void deleteByTestResultId(Long resultId);

}
