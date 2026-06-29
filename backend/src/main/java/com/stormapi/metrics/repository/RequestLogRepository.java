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

    Page<RequestLog> findByTestResultIdAndSuccessOrderByTimestampAsc(Long resultId, boolean success, Pageable pageable);

    long countByTestResultIdAndSuccess(Long resultId, boolean success);

    @Modifying
    @Transactional
    void deleteByTestResultId(Long resultId);

    /** Count request logs grouped by HTTP status code. */
    @org.springframework.data.jpa.repository.Query(
            "SELECT r.statusCode, COUNT(r) FROM RequestLog r " +
            "WHERE r.testResult.id = :resultId GROUP BY r.statusCode ORDER BY r.statusCode")
    java.util.List<Object[]> countByStatusCode(@org.springframework.data.repository.query.Param("resultId") Long resultId);

    /** Bucket response times into histogram ranges. */
    @org.springframework.data.jpa.repository.Query(
            "SELECT CASE " +
            "WHEN r.responseTimeMs < 50 THEN '0-50' " +
            "WHEN r.responseTimeMs < 100 THEN '50-100' " +
            "WHEN r.responseTimeMs < 200 THEN '100-200' " +
            "WHEN r.responseTimeMs < 500 THEN '200-500' " +
            "WHEN r.responseTimeMs < 1000 THEN '500-1000' " +
            "ELSE '1000+' END AS bucket, COUNT(r) " +
            "FROM RequestLog r WHERE r.testResult.id = :resultId GROUP BY bucket ORDER BY MIN(r.responseTimeMs)")
    java.util.List<Object[]> getResponseTimeHistogram(@org.springframework.data.repository.query.Param("resultId") Long resultId);

}
