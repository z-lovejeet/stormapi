import { memo } from 'react';
import { X, TrendingUp, TrendingDown, Minus } from 'lucide-react';
import { useComparison } from '../../hooks/useComparison';
import {
  formatMs,
  formatRps,
  formatPercent,
  formatNumber,
  formatDelta,
} from '../../utils/formatters';
import { LoadingSpinner } from '../common/LoadingSpinner';
import type { MetricDelta } from '../../types/result';
import styles from './ComparisonView.module.css';

interface ComparisonViewProps {
  resultIdA: number;
  resultIdB: number;
  onClose: () => void;
}

/**
 * Format a metric value based on its field name.
 */
function formatFieldValue(field: string, value: number): string {
  const lower = field.toLowerCase();
  if (lower.includes('ms') || lower.includes('latency') || lower.includes('time')) {
    return formatMs(value);
  }
  if (lower.includes('rps') || lower.includes('throughput')) {
    return formatRps(value);
  }
  if (lower.includes('rate') || lower.includes('percent')) {
    return formatPercent(value);
  }
  return formatNumber(value);
}

/**
 * Render the delta indicator with appropriate icon and color.
 */
function DeltaIndicator({ delta }: { delta: MetricDelta }) {
  if (delta.deltaPercent === 0) {
    return (
      <span className={`${styles.deltaIndicator} ${styles.neutral}`}>
        <Minus size={14} />
        {formatDelta(delta.deltaPercent)}
      </span>
    );
  }

  if (delta.improved) {
    return (
      <span className={`${styles.deltaIndicator} ${styles.improved}`}>
        <TrendingUp size={14} />
        {formatDelta(delta.deltaPercent)}
      </span>
    );
  }

  return (
    <span className={`${styles.deltaIndicator} ${styles.degraded}`}>
      <TrendingDown size={14} />
      {formatDelta(delta.deltaPercent)}
    </span>
  );
}

export const ComparisonView = memo(function ComparisonView({
  resultIdA,
  resultIdB,
  onClose,
}: ComparisonViewProps) {
  const { comparison, loading, error } = useComparison(resultIdA, resultIdB);

  if (loading) {
    return (
      <div className={styles.container}>
        <div className={styles.loading}>
          <LoadingSpinner size="md" />
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className={styles.container}>
        <div className={styles.header}>
          <h3 className={styles.title}>Comparison Results</h3>
          <button className={styles.closeBtn} onClick={onClose} aria-label="Close">
            <X size={18} />
          </button>
        </div>
        <div className={styles.error}>{error}</div>
      </div>
    );
  }

  if (!comparison) return null;

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <h3 className={styles.title}>Comparison Results</h3>
        <button className={styles.closeBtn} onClick={onClose} aria-label="Close">
          <X size={18} />
        </button>
      </div>

      {/* Column headers */}
      <div className={styles.deltaRow}>
        <span className={styles.columnHeader}>Metric</span>
        <span className={styles.columnHeader}>Result A</span>
        <span className={styles.columnHeader}>Result B</span>
        <span className={styles.columnHeader} style={{ textAlign: 'right' }}>
          Delta
        </span>
      </div>

      {/* Delta rows */}
      {comparison.deltas.map((delta) => (
        <div key={delta.field} className={styles.deltaRow}>
          <span className={styles.deltaLabel}>{delta.label}</span>
          <span className={styles.deltaValue}>
            {formatFieldValue(delta.field, delta.resultA)}
          </span>
          <span className={styles.deltaValue}>
            {formatFieldValue(delta.field, delta.resultB)}
          </span>
          <DeltaIndicator delta={delta} />
        </div>
      ))}
    </div>
  );
});
