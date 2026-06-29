import { memo } from 'react';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  CartesianGrid,
  Cell,
  LabelList,
} from 'recharts';
import styles from './PercentileBarChart.module.css';

interface PercentileBarChartProps {
  p50: number;
  p75: number;
  p90: number;
  p95: number;
  p99: number;
}

const PERCENTILE_DATA = [
  { key: 'p50', name: 'P50', fill: '#22c55e' },
  { key: 'p75', name: 'P75', fill: '#3b82f6' },
  { key: 'p90', name: 'P90', fill: '#f59e0b' },
  { key: 'p95', name: 'P95', fill: '#f97316' },
  { key: 'p99', name: 'P99', fill: '#ef4444' },
] as const;

export const PercentileBarChart = memo(function PercentileBarChart(
  props: PercentileBarChartProps,
) {
  const allZero = PERCENTILE_DATA.every(
    (d) => props[d.key] === 0,
  );

  if (allZero) {
    return (
      <div className={styles.container} role="img" aria-label="Response time percentiles — no data">
        <div className={styles.header}>
          <span className={styles.title}>Response Time Percentiles</span>
        </div>
        <div className={styles.emptyState}>No percentile data available</div>
      </div>
    );
  }

  const data = PERCENTILE_DATA.map((d) => ({
    name: d.name,
    value: props[d.key],
    fill: d.fill,
  }));

  return (
    <div className={styles.container} role="img" aria-label="Response time percentiles chart">
      <div className={styles.header}>
        <span className={styles.title}>Response Time Percentiles</span>
      </div>
      <div className={styles.chartWrapper}>
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={data} margin={{ top: 20, right: 8, left: -10, bottom: 0 }}>
            <CartesianGrid
              strokeDasharray="3 3"
              stroke="var(--storm-border-secondary)"
              vertical={false}
            />
            <XAxis
              dataKey="name"
              tick={{ fontSize: 10, fill: 'var(--storm-text-tertiary)' }}
              tickLine={false}
              axisLine={false}
            />
            <YAxis
              tick={{ fontSize: 10, fill: 'var(--storm-text-tertiary)' }}
              tickLine={false}
              axisLine={false}
              tickFormatter={(v: number) => `${v}ms`}
            />
            <Tooltip
              formatter={(value: number) => [`${value}ms`, 'Latency']}
              contentStyle={{
                background: 'var(--storm-bg-elevated)',
                border: '1px solid var(--storm-border-primary)',
                borderRadius: 'var(--storm-radius-md)',
                fontSize: 'var(--storm-text-xs)',
              }}
            />
            <Bar dataKey="value" radius={[4, 4, 0, 0]} isAnimationActive={false}>
              {data.map((entry) => (
                <Cell key={entry.name} fill={entry.fill} />
              ))}
              <LabelList
                dataKey="value"
                position="top"
                formatter={(v: number) => `${v}ms`}
                style={{ fontSize: 10, fill: 'var(--storm-text-secondary)' }}
              />
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
});
