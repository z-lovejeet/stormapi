import { memo } from 'react';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from 'recharts';
import { createChartTooltip, CHART_CURSOR } from './ChartTooltip';
import styles from './DonutChart.module.css';

interface DonutChartProps {
  successCount: number;
  failureCount: number;
}

const SUCCESS_COLOR = '#22c55e';
const FAILURE_COLOR = '#ef4444';

export const DonutChart = memo(function DonutChart({
  successCount,
  failureCount,
}: DonutChartProps) {
  const total = successCount + failureCount;

  if (total === 0) {
    return (
      <div className={styles.container} role="img" aria-label="Success rate chart — no data">
        <div className={styles.header}>
          <span className={styles.title}>Success Rate</span>
        </div>
        <div className={styles.emptyState}>No request data available</div>
      </div>
    );
  }

  const successRate = ((successCount / total) * 100).toFixed(1);

  const data = [
    { name: 'Success', value: successCount, color: SUCCESS_COLOR },
    { name: 'Failure', value: failureCount, color: FAILURE_COLOR },
  ];

  return (
    <div className={styles.container} role="img" aria-label={`Success rate chart — ${successRate}%`}>
      <div className={styles.header}>
        <span className={styles.title}>Success Rate</span>
      </div>
      <div className={styles.chartWrapper}>
        <ResponsiveContainer width="100%" height="100%">
          <PieChart>
            <Pie
              data={data}
              cx="50%"
              cy="50%"
              innerRadius={60}
              outerRadius={80}
              dataKey="value"
              stroke="none"
              isAnimationActive={false}
            >
              {data.map((entry) => (
                <Cell key={entry.name} fill={entry.color} />
              ))}
            </Pie>
            <Tooltip
              cursor={CHART_CURSOR}
              content={createChartTooltip({
                labelKey: 'name',
                formatEntries: (payload, name) => {
                  if (name === 'value') {
                    const color = (payload.payload as any).color || SUCCESS_COLOR;
                    return { label: 'Requests', value: Number(payload.value).toLocaleString(), color };
                  }
                  return null;
                },
              })}
            />
          </PieChart>
        </ResponsiveContainer>
        <div className={styles.centerLabel}>
          <div className={styles.centerValue}>{successRate}%</div>
          <div className={styles.centerSubtitle}>success</div>
        </div>
      </div>
      <div className={styles.legend}>
        <div className={styles.legendItem}>
          <span className={styles.legendDot} style={{ background: SUCCESS_COLOR }} />
          Success ({successCount.toLocaleString()})
        </div>
        <div className={styles.legendItem}>
          <span className={styles.legendDot} style={{ background: FAILURE_COLOR }} />
          Failure ({failureCount.toLocaleString()})
        </div>
      </div>
    </div>
  );
});
