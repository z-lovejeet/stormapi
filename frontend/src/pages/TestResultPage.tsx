import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Clock, Calendar, Users } from 'lucide-react';
import { useTestResult } from '../hooks/useTestResult';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { EmptyState } from '../components/common/EmptyState';
import { StatusBadge } from '../components/common/StatusBadge';
import { TestTypeBadge } from '../components/common/TestTypeBadge';
import { ExportDropdown } from '../components/export/ExportDropdown';
import { ResultSummaryCards } from '../components/results/ResultSummaryCards';
import { MetricsDetailTable } from '../components/results/MetricsDetailTable';
import { DonutChart } from '../components/charts/DonutChart';
import { PercentileBarChart } from '../components/charts/PercentileBarChart';
import { StatusCodeChart } from '../components/charts/StatusCodeChart';
import { TimelineChart } from '../components/charts/TimelineChart';
import { ResponseTimeHistogram } from '../components/charts/ResponseTimeHistogram';
import { ScalabilityCurve } from '../components/charts/ScalabilityCurve';
import { formatDuration, formatDateTime } from '../utils/formatters';
import styles from './TestResultPage.module.css';

export function TestResultPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const testId = Number(id);

  const {
    testConfig,
    result,
    snapshots,
    statusCodes,
    histogram,
    loading,
    error,
  } = useTestResult(testId);

  if (loading) {
    return (
      <div className={styles.loadingWrapper}>
        <LoadingSpinner />
        <span>Loading test results…</span>
      </div>
    );
  }

  if (error) {
    return (
      <div className={styles.errorWrapper}>
        <span>Failed to load results: {error}</span>
        <button className={styles.backBtn} onClick={() => navigate('/dashboard')}>
          <ArrowLeft size={16} /> Back to Dashboard
        </button>
      </div>
    );
  }

  if (!result) {
    return (
      <EmptyState
        icon={Clock}
        title="No Results Yet"
        description="This test hasn't been run yet or results aren't available."
      />
    );
  }

  const durationSec = result.durationMs ? Math.round(result.durationMs / 1000) : 0;

  return (
    <div className={styles.page}>
      {/* ── Header ──────────────────────────────── */}
      <div className={styles.header}>
        <div className={styles.headerLeft}>
          <button className={styles.backBtn} onClick={() => navigate(-1)}>
            <ArrowLeft size={16} />
          </button>
          <h1>Test Results</h1>
          <StatusBadge status={result.status} />
          {testConfig && <TestTypeBadge type={testConfig.testType} />}
        </div>
        <div className={styles.headerRight}>
          <ExportDropdown resultId={result.id} />
        </div>
      </div>

      {/* ── Meta info ───────────────────────────── */}
      <div className={styles.meta}>
        {result.startedAt && (
          <span className={styles.metaItem}>
            <Calendar size={14} />
            {formatDateTime(result.startedAt)}
          </span>
        )}
        {durationSec > 0 && (
          <span className={styles.metaItem}>
            <Clock size={14} />
            {formatDuration(durationSec)}
          </span>
        )}
        {testConfig && (
          <span className={styles.metaItem}>
            <Users size={14} />
            {testConfig.virtualUsers} users
          </span>
        )}
      </div>

      {/* ── Summary Cards ───────────────────────── */}
      <div className={styles.section}>
        <ResultSummaryCards result={result} />
      </div>

      {/* ── Timeline Chart (full width) ─────────── */}
      <div className={styles.section}>
        <h2 className={styles.sectionTitle}>Performance Timeline</h2>
        <TimelineChart snapshots={snapshots} />
      </div>

      {/* ── Chart Grid ──────────────────────────── */}
      <div className={styles.chartGrid}>
        <PercentileBarChart
          p50={result.p50Ms}
          p75={result.p75Ms}
          p90={result.p90Ms}
          p95={result.p95Ms}
          p99={result.p99Ms}
        />
        <DonutChart
          successCount={result.successCount}
          failureCount={result.failureCount}
        />
        <StatusCodeChart data={statusCodes} />
        <ResponseTimeHistogram data={histogram} />
      </div>

      {/* ── Scalability Curve ────────────────────── */}
      {snapshots.length > 0 && (
        <div className={styles.section}>
          <h2 className={styles.sectionTitle}>Scalability Curve</h2>
          <ScalabilityCurve snapshots={snapshots} />
        </div>
      )}

      {/* ── Detailed Metrics Table ────────────────── */}
      <div className={styles.section}>
        <h2 className={styles.sectionTitle}>Detailed Metrics</h2>
        <MetricsDetailTable result={result} />
      </div>
    </div>
  );
}
