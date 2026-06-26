import { memo, useId } from 'react';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  Area,
  CartesianGrid,
} from 'recharts';
import styles from './LiveLineChart.module.css';

export interface ChartDataPoint {
  time: string;
  value: number;
}

interface LiveLineChartProps {
  data: ChartDataPoint[];
  label: string;
  color: string;
  unit?: string;
  formatValue?: (v: number) => string;
  currentValue?: string;
  animate?: boolean;
}

export const LiveLineChart = memo(function LiveLineChart({
  data,
  label,
  color,
  unit = '',
  formatValue,
  currentValue,
  animate = false,
}: LiveLineChartProps) {
  const gradientId = useId();

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const tooltipFormatter = (value: any) => {
    const num = Number(value) || 0;
    const formatted = formatValue ? formatValue(num) : `${num}`;
    return [formatted, label];
  };

  if (data.length === 0) {
    return (
      <div className={styles.container} role="img" aria-label={`${label} chart — waiting for data`}>
        <div className={styles.header}>
          <span className={styles.title}>{label}</span>
        </div>
        <div className={styles.emptyState}>Waiting for data…</div>
      </div>
    );
  }

  return (
    <div className={styles.container} role="img" aria-label={`${label} chart`}>
      <div className={styles.header}>
        <span className={styles.title}>{label}</span>
        {currentValue && (
          <span className={styles.currentValue} style={{ color }}>
            {currentValue}
            {unit && ` ${unit}`}
          </span>
        )}
      </div>
      <div className={styles.chartWrapper}>
        <ResponsiveContainer width="100%" height="100%">
          <LineChart data={data} margin={{ top: 4, right: 4, left: -20, bottom: 0 }}>
            <defs>
              <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor={color} stopOpacity={0.15} />
                <stop offset="100%" stopColor={color} stopOpacity={0.02} />
              </linearGradient>
            </defs>
            <CartesianGrid
              strokeDasharray="3 3"
              stroke="var(--storm-border-secondary)"
              vertical={false}
            />
            <XAxis
              dataKey="time"
              tick={{ fontSize: 10, fill: 'var(--storm-text-tertiary)' }}
              tickLine={false}
              axisLine={false}
              interval="preserveStartEnd"
            />
            <YAxis
              tick={{ fontSize: 10, fill: 'var(--storm-text-tertiary)' }}
              tickLine={false}
              axisLine={false}
              tickFormatter={formatValue}
            />
            <Tooltip
              formatter={tooltipFormatter}
              contentStyle={{
                background: 'var(--storm-bg-elevated)',
                border: '1px solid var(--storm-border-primary)',
                borderRadius: 'var(--storm-radius-md)',
                fontSize: 'var(--storm-text-xs)',
              }}
            />
            <Area
              type="monotone"
              dataKey="value"
              stroke="none"
              fill={`url(#${gradientId})`}
              isAnimationActive={false}
            />
            <Line
              type="monotone"
              dataKey="value"
              stroke={color}
              strokeWidth={2}
              dot={false}
              activeDot={{ r: 4, fill: color }}
              isAnimationActive={animate}
            />
          </LineChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
});
