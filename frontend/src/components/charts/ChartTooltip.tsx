import styles from './ChartTooltip.module.css';

export interface TooltipEntry {
  label: string;
  value: string;
  color: string;
}

type Formatter = (
  payload: Record<string, unknown>,
  name: string,
) => TooltipEntry | null;

interface ChartTooltipProps {
  labelKey?: string;
  formatLabel?: (value: unknown) => string;
  formatEntries: Formatter;
}

/**
 * Premium shared tooltip for all StormAPI charts.
 *
 * Returns a Recharts-compatible `content` render function.
 * Usage: <Tooltip content={createChartTooltip({ ... })} />
 */
export function createChartTooltip({
  labelKey,
  formatLabel,
  formatEntries,
}: ChartTooltipProps) {
  return function CustomTooltip({
    active,
    payload,
    label,
  }: any) {
    if (!active || !payload || payload.length === 0) return null;

    const headerValue = labelKey
      ? payload[0]?.payload?.[labelKey]
      : label;

    const header = formatLabel
      ? formatLabel(headerValue)
      : headerValue != null
        ? String(headerValue)
        : undefined;

    const entries: TooltipEntry[] = [];

    for (const item of payload) {
      if (item.dataKey == null) continue;
      const entry = formatEntries(
        item.payload as Record<string, unknown>,
        String(item.dataKey),
      );
      if (entry) entries.push(entry);
    }

    if (entries.length === 0) return null;

    return (
      <div className={styles.tooltip}>
        {header && <div className={styles.tooltipHeader}>{header}</div>}
        <div className={styles.tooltipBody}>
          {entries.map((entry, i) => (
            <div className={styles.tooltipRow} key={i}>
              <span className={styles.tooltipLabel}>
                <span
                  className={styles.tooltipDot}
                  style={{ background: entry.color, color: entry.color }}
                />
                {entry.label}
              </span>
              <span className={styles.tooltipValue}>{entry.value}</span>
            </div>
          ))}
        </div>
      </div>
    );
  };
}

/** Shared cursor style for chart hover lines */
export const CHART_CURSOR = {
  stroke: 'rgba(255, 255, 255, 0.08)',
  strokeWidth: 1,
  strokeDasharray: '4 4',
};
