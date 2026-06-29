import { memo } from 'react';
import {
  formatMs,
  formatRps,
  formatPercent,
  formatNumber,
  formatBytes,
  formatDuration,
  formatDateTime,
} from '../../utils/formatters';
import type { TestResultResponse } from '../../types/test';
import styles from './MetricsDetailTable.module.css';

interface MetricsDetailTableProps {
  result: TestResultResponse;
}

interface MetricRow {
  label: string;
  value: string;
}

interface MetricSection {
  header: string;
  rows: MetricRow[];
}

export const MetricsDetailTable = memo(function MetricsDetailTable({
  result,
}: MetricsDetailTableProps) {
  const sections: MetricSection[] = [
    {
      header: 'Counts',
      rows: [
        { label: 'Total Requests', value: formatNumber(result.totalRequests) },
        { label: 'Successful', value: formatNumber(result.successCount) },
        { label: 'Failed', value: formatNumber(result.failureCount) },
      ],
    },
    {
      header: 'Latency',
      rows: [
        { label: 'Average', value: formatMs(result.avgResponseTimeMs) },
        { label: 'Minimum', value: formatMs(result.minResponseTimeMs) },
        { label: 'Maximum', value: formatMs(result.maxResponseTimeMs) },
        { label: 'P50', value: formatMs(result.p50Ms) },
        { label: 'P75', value: formatMs(result.p75Ms) },
        { label: 'P90', value: formatMs(result.p90Ms) },
        { label: 'P95', value: formatMs(result.p95Ms) },
        { label: 'P99', value: formatMs(result.p99Ms) },
      ],
    },
    {
      header: 'Performance',
      rows: [
        { label: 'Throughput', value: formatRps(result.throughputRps) },
        { label: 'Error Rate', value: formatPercent(result.errorRate) },
        { label: 'Total Data Transferred', value: formatBytes(result.totalDataBytes) },
      ],
    },
    {
      header: 'Timing',
      rows: [
        { label: 'Duration', value: formatDuration(result.durationMs / 1000) },
        { label: 'Started At', value: formatDateTime(result.startedAt) },
        ...(result.completedAt
          ? [{ label: 'Completed At', value: formatDateTime(result.completedAt) }]
          : []),
      ],
    },
  ];

  // Conditionally add type-specific metrics
  const typeSpecificRows: MetricRow[] = [];
  if (result.breakpointUsers !== undefined) {
    typeSpecificRows.push({ label: 'Breakpoint Users', value: formatNumber(result.breakpointUsers) });
  }
  if (result.recoveryTimeMs !== undefined) {
    typeSpecificRows.push({ label: 'Recovery Time', value: formatMs(result.recoveryTimeMs) });
  }
  if (result.degradationSlope !== undefined) {
    typeSpecificRows.push({ label: 'Degradation Slope', value: result.degradationSlope.toFixed(4) });
  }
  if (result.degradationDetected !== undefined) {
    typeSpecificRows.push({ label: 'Degradation Detected', value: result.degradationDetected ? 'Yes' : 'No' });
  }
  if (typeSpecificRows.length > 0) {
    sections.push({ header: 'Type-Specific', rows: typeSpecificRows });
  }

  return (
    <div className={styles.container}>
      <h3 className={styles.title}>Detailed Metrics</h3>
      <div className={styles.table}>
        {sections.map((section) => (
          <div key={section.header}>
            <div className={styles.sectionHeader}>{section.header}</div>
            {section.rows.map((row) => (
              <div key={row.label} className={styles.row}>
                <span className={styles.label}>{row.label}</span>
                <span className={styles.value}>{row.value}</span>
              </div>
            ))}
          </div>
        ))}
      </div>
    </div>
  );
});
