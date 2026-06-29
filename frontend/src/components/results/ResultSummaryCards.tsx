import { memo } from 'react';
import { Activity, Clock, AlertTriangle, Zap, CheckCircle, BarChart3 } from 'lucide-react';
import { KpiCard } from '../common/KpiCard';
import {
  formatMs,
  formatRps,
  formatPercent,
  formatNumber,
  formatSuccessRate,
} from '../../utils/formatters';
import type { TestResultResponse } from '../../types/test';
import styles from './ResultSummaryCards.module.css';

interface ResultSummaryCardsProps {
  result: TestResultResponse;
}

export const ResultSummaryCards = memo(function ResultSummaryCards({
  result,
}: ResultSummaryCardsProps) {
  const passed = result.errorRate < 5;

  return (
    <div>
      {/* Status banner */}
      <div
        className={`${styles.banner} ${passed ? styles.bannerSuccess : styles.bannerFailed}`}
      >
        {passed ? (
          <CheckCircle size={18} />
        ) : (
          <AlertTriangle size={18} />
        )}
        {passed ? 'Test Passed' : 'Test Failed'}
      </div>

      {/* KPI grid */}
      <div className={styles.grid}>
        <KpiCard
          icon={Activity}
          label="Total Requests"
          value={formatNumber(result.totalRequests)}
        />
        <KpiCard
          icon={CheckCircle}
          label="Success Rate"
          value={formatSuccessRate(result.successCount, result.totalRequests)}
        />
        <KpiCard
          icon={Clock}
          label="Avg Response Time"
          value={formatMs(result.avgResponseTimeMs)}
        />
        <KpiCard
          icon={BarChart3}
          label="P95 Latency"
          value={formatMs(result.p95Ms)}
        />
        <KpiCard
          icon={Zap}
          label="Throughput"
          value={formatRps(result.throughputRps)}
        />
        <KpiCard
          icon={AlertTriangle}
          label="Error Rate"
          value={formatPercent(result.errorRate)}
        />
      </div>
    </div>
  );
});
