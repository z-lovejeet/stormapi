import { Zap, TrendingUp, Activity, Timer, Target, BarChart3 } from 'lucide-react';
import type { LucideIcon } from 'lucide-react';
import { TestType } from '../../types/test';

const TYPE_CONFIG: Record<TestType, { icon: LucideIcon; label: string; color: string }> = {
  [TestType.LOAD]: { icon: Zap, label: 'Load', color: 'var(--storm-chart-1)' },
  [TestType.STRESS]: { icon: TrendingUp, label: 'Stress', color: 'var(--storm-chart-2)' },
  [TestType.SPIKE]: { icon: Activity, label: 'Spike', color: 'var(--storm-chart-3)' },
  [TestType.SOAK]: { icon: Timer, label: 'Soak', color: 'var(--storm-chart-4)' },
  [TestType.BREAKPOINT]: { icon: Target, label: 'Breakpoint', color: 'var(--storm-chart-5)' },
  [TestType.SCALABILITY]: { icon: BarChart3, label: 'Scalability', color: 'var(--storm-chart-6)' },
};

interface TestTypeBadgeProps {
  type: TestType;
}

export function TestTypeBadge({ type }: TestTypeBadgeProps) {
  const config = TYPE_CONFIG[type];
  const Icon = config.icon;

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
        color: config.color,
        background: `color-mix(in srgb, ${config.color} 12%, transparent)`,
        lineHeight: '1.6',
      }}
    >
      <Icon size={12} strokeWidth={2.5} />
      {config.label}
    </span>
  );
}
