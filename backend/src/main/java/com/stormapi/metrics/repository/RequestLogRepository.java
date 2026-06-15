package com.stormapi.metrics.repository;

import com.stormapi.metrics.model.RequestLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Data JPA repository for RequestLog entity.
 * All read queries MUST use pagination due to potentially large row counts (10,000+ per test).
 */
@Repository
public interface RequestLogRepository extends JpaRepository<RequestLog, Long> {

    Page<RequestLog> findByTestResultIdOrderByTimestampAsc(Long resultId, Pageable pageable);

    long countByTestResultIdAndSuccess(Long resultId, boolean success);

    @Modifying
    @Transactional
    void deleteByTestResultId(Long resultId);

}
