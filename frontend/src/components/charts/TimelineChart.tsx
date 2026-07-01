import { memo, useState, useId } from 'react';
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  CartesianGrid,
} from 'recharts';
import styles from './TimelineChart.module.css';

interface Snapshot {
  timestamp: string;
  activeUsers: number;
  requestsPerSecond: number;
  avgResponseTimeMs: number;
  errorRate: number;
  p95Ms: number;
  cumulativeRequests: number;
  cumulativeErrors: number;
}

interface TimelineChartProps {
  snapshots: Snapshot[];
  metric?: string;
}

const METRIC_CONFIG: Record<string, { label: string; color: string; unit: string }> = {
  avgResponseTimeMs: { label: 'Avg Response', color: '#3b82f6', unit: 'ms' },
  requestsPerSecond: { label: 'RPS', color: '#22c55e', unit: 'req/s' },
  errorRate: { label: 'Error Rate', color: '#ef4444', unit: '%' },
  p95Ms: { label: 'P95', color: '#f59e0b', unit: 'ms' },
  activeUsers: { label: 'Active Users', color: '#8b5cf6', unit: '' },
};

const METRIC_KEYS = Object.keys(METRIC_CONFIG);

export const TimelineChart = memo(function TimelineChart({
  snapshots,
  metric = 'avgResponseTimeMs',
}: TimelineChartProps) {
  const [selectedMetric, setSelectedMetric] = useState(metric);
  const gradientId = useId();

  const config = METRIC_CONFIG[selectedMetric] ?? METRIC_CONFIG.avgResponseTimeMs;

  if (snapshots.length === 0) {
    return (
      <div className={styles.container} role="img" aria-label="Timeline chart — no data">
        <div className={styles.header}>
          <span className={styles.title}>Timeline</span>
        </div>
        <div className={styles.emptyState}>No timeline data available</div>
      </div>
    );
  }

  const formatTimestamp = (ts: string) => {
    try {
      const d = new Date(ts);
      return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
    } catch {
      return ts;
    }
  };

  return (
    <div className={styles.container} role="img" aria-label={`Timeline chart — ${config.label}`}>
      <div className={styles.header}>
        <span className={styles.title}>Timeline</span>
        <div className={styles.metricTabs}>
          {METRIC_KEYS.map((key) => (
            <button
              key={key}
              className={`${styles.metricTab} ${selectedMetric === key ? styles.metricTabActive : ''}`}
              onClick={() => setSelectedMetric(key)}
            >
              {METRIC_CONFIG[key].label}
            </button>
          ))}
        </div>
      </div>
      <div className={styles.chartWrapper}>
        <ResponsiveContainer width="100%" height="100%">
          <AreaChart data={snapshots} margin={{ top: 4, right: 8, left: -10, bottom: 0 }}>
            <defs>
              <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor={config.color} stopOpacity={0.2} />
                <stop offset="100%" stopColor={config.color} stopOpacity={0.02} />
              </linearGradient>
            </defs>
            <CartesianGrid
              strokeDasharray="3 3"
              stroke="var(--storm-border-secondary)"
              vertical={false}
            />
            <XAxis
              dataKey="timestamp"
              tick={{ fontSize: 10, fill: 'var(--storm-text-tertiary)' }}
              tickLine={false}
              axisLine={false}
              tickFormatter={formatTimestamp}
              interval="preserveStartEnd"
            />
            <YAxis
              tick={{ fontSize: 10, fill: 'var(--storm-text-tertiary)' }}
              tickLine={false}
              axisLine={false}
              tickFormatter={(v: number) =>
                config.unit ? `${v}${config.unit}` : String(v)
              }
            />
            <Tooltip
              labelFormatter={formatTimestamp}
              formatter={(value: number) => [
                config.unit ? `${value}${config.unit}` : value,
                config.label,
              ]}
              contentStyle={{
                background: 'var(--storm-bg-elevated)',
                border: '1px solid var(--storm-border-primary)',
                borderRadius: 'var(--storm-radius-md)',
                fontSize: 'var(--storm-text-xs)',
              }}
            />
            <Area
              type="monotone"
              dataKey={selectedMetric}
              stroke={config.color}
              strokeWidth={2}
              fill={`url(#${gradientId})`}
              dot={false}
              activeDot={{ r: 4, fill: config.color }}
              isAnimationActive={false}
            />
          </AreaChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
});
