import { memo } from 'react';
import { formatDuration } from '../../utils/formatters';
import styles from './ProgressBar.module.css';

interface ProgressBarProps {
  elapsedSeconds: number;
  totalSeconds: number;
  status: 'running' | 'completed' | 'stopped';
}

export const ProgressBar = memo(function ProgressBar({
  elapsedSeconds,
  totalSeconds,
  status,
}: ProgressBarProps) {
  const percentage = totalSeconds > 0 ? Math.min(100, (elapsedSeconds / totalSeconds) * 100) : 0;
  const remaining = Math.max(0, totalSeconds - elapsedSeconds);

  const fillClass = status === 'completed'
    ? styles.completed
    : status === 'stopped'
      ? styles.stopped
      : '';

  return (
    <div className={styles.container}>
      <div className={styles.labels}>
        <span className={styles.elapsed}>
          {formatDuration(elapsedSeconds)} / {formatDuration(totalSeconds)}
        </span>
        <span className={styles.remaining}>
          {status === 'completed'
            ? 'Completed'
            : status === 'stopped'
              ? 'Stopped'
              : `${formatDuration(remaining)} remaining`}
        </span>
      </div>
      <div
        className={styles.track}
        role="progressbar"
        aria-valuenow={Math.round(percentage)}
        aria-valuemin={0}
        aria-valuemax={100}
        aria-label="Test progress"
      >
        <div
          className={`${styles.fill} ${fillClass}`}
          style={{ width: `${percentage}%` }}
        />
      </div>
      <div className={styles.percentage}>{percentage.toFixed(1)}%</div>
    </div>
  );
});
