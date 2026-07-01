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
} from 'recharts';
import styles from './ResponseTimeHistogram.module.css';

const BUCKET_COLORS = ['#22c55e', '#84cc16', '#f59e0b', '#f97316', '#ef4444', '#dc2626'];

interface HistogramBucket {
  range: string;
  count: number;
}

interface ResponseTimeHistogramProps {
  data: HistogramBucket[];
}

export const ResponseTimeHistogram = memo(function ResponseTimeHistogram({
  data,
}: ResponseTimeHistogramProps) {
  if (!data || data.length === 0) {
    return (
      <div className={styles.container} role="img" aria-label="Response time histogram — no data">
        <div className={styles.header}>
          <span className={styles.title}>Response Time Distribution</span>
        </div>
        <div className={styles.emptyState}>No data available</div>
      </div>
    );
  }

  return (
    <div className={styles.container} role="img" aria-label="Response time histogram">
      <div className={styles.header}>
        <span className={styles.title}>Response Time Distribution</span>
      </div>
      <div className={styles.chartWrapper}>
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={data} margin={{ top: 8, right: 8, left: -10, bottom: 0 }}>
            <CartesianGrid
              strokeDasharray="3 3"
              stroke="var(--storm-border-secondary)"
              vertical={false}
            />
            <XAxis
              dataKey="range"
              tick={{ fontSize: 10, fill: 'var(--storm-text-tertiary)' }}
              tickLine={false}
              axisLine={false}
            />
            <YAxis
              tick={{ fontSize: 10, fill: 'var(--storm-text-tertiary)' }}
              tickLine={false}
              axisLine={false}
            />
            <Tooltip
              contentStyle={{
                background: 'var(--storm-bg-elevated)',
                border: '1px solid var(--storm-border-primary)',
                borderRadius: 'var(--storm-radius-md)',
                fontSize: 'var(--storm-text-xs)',
              }}
              formatter={(value: number) => [`${value} requests`, 'Count']}
            />
            <Bar dataKey="count" radius={[4, 4, 0, 0]} isAnimationActive={false}>
              {data.map((_, i) => (
                <Cell key={i} fill={BUCKET_COLORS[i % BUCKET_COLORS.length]} />
              ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
});
