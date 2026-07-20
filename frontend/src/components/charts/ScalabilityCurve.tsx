import { memo } from 'react';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  CartesianGrid,
} from 'recharts';
import { formatMs, formatRps } from '../../utils/formatters';
import { createChartTooltip, CHART_CURSOR } from './ChartTooltip';
import styles from './ScalabilityCurve.module.css';

interface DataPoint {
  activeUsers: number;
  requestsPerSecond: number;
  avgResponseTimeMs: number;
}

interface ScalabilityCurveProps {
  snapshots: DataPoint[];
}

export const ScalabilityCurve = memo(function ScalabilityCurve({
  snapshots,
}: ScalabilityCurveProps) {
  if (!snapshots || snapshots.length === 0) {
    return (
      <div className={styles.container} role="img" aria-label="Scalability curve — no data">
        <div className={styles.header}>
          <span className={styles.title}>Scalability Curve</span>
        </div>
        <div className={styles.emptyState}>No data available</div>
      </div>
    );
  }

  // Deduplicate by activeUsers — take last entry per user count
  const byUser = new Map<number, DataPoint>();
  snapshots.forEach((s) => byUser.set(s.activeUsers, s));
  const data = Array.from(byUser.values()).sort((a, b) => a.activeUsers - b.activeUsers);

  return (
    <div className={styles.container} role="img" aria-label="Scalability curve">
      <div className={styles.header}>
        <span className={styles.title}>Scalability Curve</span>
      </div>
      <div className={styles.chartWrapper}>
        <ResponsiveContainer width="100%" height="100%">
          <LineChart data={data} margin={{ top: 8, right: 8, left: -10, bottom: 0 }}>
            <CartesianGrid
              strokeDasharray="3 3"
              stroke="var(--storm-border-secondary)"
              vertical={false}
            />
            <XAxis
              dataKey="activeUsers"
              tick={{ fontSize: 10, fill: 'var(--storm-text-tertiary)' }}
              tickLine={false}
              axisLine={false}
              label={{ value: 'Users', position: 'insideBottom', offset: -2, fontSize: 10, fill: 'var(--storm-text-tertiary)' }}
            />
            <YAxis
              yAxisId="left"
              tick={{ fontSize: 10, fill: 'var(--storm-text-tertiary)' }}
              tickLine={false}
              axisLine={false}
              tickFormatter={(v) => formatRps(v)}
            />
            <YAxis
              yAxisId="right"
              orientation="right"
              tick={{ fontSize: 10, fill: 'var(--storm-text-tertiary)' }}
              tickLine={false}
              axisLine={false}
              tickFormatter={(v) => formatMs(v)}
            />
            <Tooltip
              cursor={CHART_CURSOR}
              content={createChartTooltip({
                labelKey: 'activeUsers',
                formatLabel: (v) => `${v} users`,
                formatEntries: (payload, name) => {
                  if (name === 'requestsPerSecond') return { label: 'Throughput', value: formatRps(Number(payload.requestsPerSecond)), color: '#22c55e' };
                  if (name === 'avgResponseTimeMs') return { label: 'Latency', value: formatMs(Number(payload.avgResponseTimeMs)), color: '#3b82f6' };
                  return null;
                },
              })}
            />
            <Line
              yAxisId="left"
              type="monotone"
              dataKey="requestsPerSecond"
              stroke="#22c55e"
              strokeWidth={2}
              dot={false}
              activeDot={{ r: 4, fill: '#22c55e' }}
              isAnimationActive={false}
            />
            <Line
              yAxisId="right"
              type="monotone"
              dataKey="avgResponseTimeMs"
              stroke="#3b82f6"
              strokeWidth={2}
              dot={false}
              activeDot={{ r: 4, fill: '#3b82f6' }}
              isAnimationActive={false}
            />
          </LineChart>
        </ResponsiveContainer>
      </div>
      <div className={styles.legend}>
        <div className={styles.legendItem}>
          <span className={styles.legendDot} style={{ background: '#22c55e' }} />
          Throughput (RPS)
        </div>
        <div className={styles.legendItem}>
          <span className={styles.legendDot} style={{ background: '#3b82f6' }} />
          Avg Latency (ms)
        </div>
      </div>
    </div>
  );
});
