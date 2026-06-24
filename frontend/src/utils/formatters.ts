/**
 * Format milliseconds to human-readable string.
 * <1ms → "0.5ms", <1000ms → "123ms", ≥1000ms → "1.23s"
 */
export function formatMs(ms: number): string {
  if (ms < 0) return '0ms';
  if (ms < 1) return `${ms.toFixed(1)}ms`;
  if (ms < 1000) return `${Math.round(ms)}ms`;
  return `${(ms / 1000).toFixed(2)}s`;
}

/**
 * Format requests per second with appropriate precision.
 */
export function formatRps(rps: number): string {
  if (rps < 0) return '0 rps';
  if (rps < 1) return `${rps.toFixed(2)} rps`;
  if (rps < 100) return `${rps.toFixed(1)} rps`;
  return `${Math.round(rps).toLocaleString()} rps`;
}

/**
 * Format bytes to human-readable string (KB, MB, GB).
 */
export function formatBytes(bytes: number): string {
  if (bytes < 0) return '0 B';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
  return `${(bytes / (1024 * 1024 * 1024)).toFixed(2)} GB`;
}

/**
 * Format a number with locale-aware separators.
 */
export function formatNumber(n: number): string {
  return n.toLocaleString();
}

/**
 * Format a percentage with 1 decimal.
 */
export function formatPercent(value: number): string {
  return `${value.toFixed(1)}%`;
}

/**
 * Format an ISO date string to locale short format.
 */
export function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString(undefined, {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  });
}

/**
 * Format an ISO date string to locale date+time.
 */
export function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString(undefined, {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

/**
 * Format duration in seconds to mm:ss or hh:mm:ss.
 */
export function formatDuration(seconds: number): string {
  if (seconds < 0) return '0:00';
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  const s = Math.floor(seconds % 60);
  if (h > 0) return `${h}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
  return `${m}:${String(s).padStart(2, '0')}`;
}
