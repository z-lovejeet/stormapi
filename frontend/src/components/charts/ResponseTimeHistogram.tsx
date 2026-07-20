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
import { createChartTooltip, CHART_CURSOR } from './ChartTooltip';
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
              cursor={CHART_CURSOR}
              content={createChartTooltip({
                labelKey: 'range',
                formatEntries: (payload, name) => {
                  if (name === 'count') {
                    // Match the bucket color dynamically based on the index or fallback
                    // A simple way is to use a fixed color or extract it, but Recharts provides it in payload.fill
                    const color = (payload.fill as string) || '#22c55e';
                    return { label: 'Count', value: `${Number(payload.count)} requests`, color };
                  }
                  return null;
                },
              })}
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
