import React, { memo } from 'react';
import { CheckCircle, XCircle, Shield } from 'lucide-react';
import type { AssertionResult } from '../../types/data';
import styles from './AssertionResultBadge.module.css';

interface AssertionResultBadgeProps {
  results: AssertionResult[];
}

/**
 * Compact badge displaying assertion pass/fail count with hover tooltip.
 */
const AssertionResultBadge: React.FC<AssertionResultBadgeProps> = ({ results }) => {
  if (!results || results.length === 0) return null;

  const passCount = results.filter((r) => r.passed).length;
  const failCount = results.length - passCount;

  const badgeClass = failCount === 0 ? styles.passed
    : passCount === 0 ? styles.failed
    : styles.mixed;

  return (
    <div className={styles.tooltip}>
      <span className={`${styles.badge} ${badgeClass}`}>
        <Shield size={12} />
        {passCount}/{results.length}
      </span>
      <div className={styles.tooltipContent}>
        {results.map((r, i) => (
          <div
            key={i}
            className={`${styles.tooltipRow} ${r.passed ? styles.tooltipPass : styles.tooltipFail}`}
          >
            {r.passed ? <CheckCircle size={12} /> : <XCircle size={12} />}
            {r.assertionType}: {r.message}
          </div>
        ))}
      </div>
    </div>
  );
};

export default memo(AssertionResultBadge);
