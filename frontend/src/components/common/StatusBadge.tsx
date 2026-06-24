import { TestStatus } from '../../types/test';

const STATUS_STYLES: Record<TestStatus, { bg: string; color: string; label: string }> = {
  [TestStatus.CREATED]: { bg: 'var(--storm-bg-tertiary)', color: 'var(--storm-text-secondary)', label: 'Created' },
  [TestStatus.QUEUED]: { bg: 'var(--storm-info-subtle)', color: 'var(--storm-info)', label: 'Queued' },
  [TestStatus.RUNNING]: { bg: 'var(--storm-accent-primary-subtle)', color: 'var(--storm-accent-primary)', label: 'Running' },
  [TestStatus.COMPLETED]: { bg: 'var(--storm-success-subtle)', color: 'var(--storm-success)', label: 'Completed' },
  [TestStatus.FAILED]: { bg: 'var(--storm-error-subtle)', color: 'var(--storm-error)', label: 'Failed' },
  [TestStatus.CANCELLED]: { bg: 'var(--storm-warning-subtle)', color: 'var(--storm-warning)', label: 'Cancelled' },
};

interface StatusBadgeProps {
  status: TestStatus;
}

export function StatusBadge({ status }: StatusBadgeProps) {
  const config = STATUS_STYLES[status];

  return (
    <span
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: '6px',
        padding: '2px 10px',
        borderRadius: 'var(--storm-radius-full)',
        fontSize: 'var(--storm-text-xs)',
        fontWeight: 'var(--storm-weight-medium)',
        background: config.bg,
        color: config.color,
        lineHeight: '1.6',
      }}
    >
      <span
        style={{
          width: 6,
          height: 6,
          borderRadius: '50%',
          background: config.color,
          flexShrink: 0,
        }}
      />
      {config.label}
    </span>
  );
}
