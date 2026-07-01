import { memo, useMemo } from 'react';
import { PieChart, Pie, Cell, Tooltip, ResponsiveContainer } from 'recharts';
import styles from './StatusCodeChart.module.css';

interface StatusCodeChartProps {
  data: Record<number, number>;
}

function getStatusColor(code: number): string {
  const prefix = Math.floor(code / 100);
  switch (prefix) {
    case 2:
      return '#22c55e';
    case 3:
      return '#3b82f6';
    case 4:
      return '#f59e0b';
    case 5:
      return '#ef4444';
    default:
      return '#6b7280';
  }
}

export const StatusCodeChart = memo(function StatusCodeChart({
  data,
}: StatusCodeChartProps) {
  const chartData = useMemo(() => {
    return Object.entries(data).map(([code, count]) => ({
      name: String(code),
      value: count,
      fill: getStatusColor(Number(code)),
    }));
  }, [data]);

  if (chartData.length === 0) {
    return (
      <div className={styles.container} role="img" aria-label="Status code distribution — no data">
        <div className={styles.header}>
          <span className={styles.title}>Status Code Distribution</span>
        </div>
        <div className={styles.emptyState}>No status code data available</div>
      </div>
    );
  }

  return (
    <div className={styles.container} role="img" aria-label="Status code distribution chart">
      <div className={styles.header}>
        <span className={styles.title}>Status Code Distribution</span>
      </div>
      <div className={styles.chartWrapper}>
        <ResponsiveContainer width="100%" height="100%">
          <PieChart>
            <Pie
              data={chartData}
              cx="50%"
              cy="50%"
              innerRadius={60}
              outerRadius={80}
              dataKey="value"
              stroke="none"
              isAnimationActive={false}
            >
              {chartData.map((entry) => (
                <Cell key={entry.name} fill={entry.fill} />
              ))}
            </Pie>
            <Tooltip
              // eslint-disable-next-line @typescript-eslint/no-explicit-any
              formatter={(value: any, name: any) => [Number(value).toLocaleString(), `HTTP ${name}`]}
              contentStyle={{
                background: 'var(--storm-bg-elevated)',
                border: '1px solid var(--storm-border-primary)',
                borderRadius: 'var(--storm-radius-md)',
                fontSize: 'var(--storm-text-xs)',
              }}
            />
          </PieChart>
        </ResponsiveContainer>
      </div>
      <div className={styles.legend}>
        {chartData.map((entry) => (
          <div key={entry.name} className={styles.legendItem}>
            <span className={styles.legendDot} style={{ background: entry.fill }} />
            {entry.name} ({entry.value.toLocaleString()})
          </div>
        ))}
      </div>
    </div>
  );
});
