import type { TestResultResponse } from './test';

/**
 * Delta between two metric values for comparison view.
 */
export interface MetricDelta {
  field: string;
  label: string;
  resultA: number;
  resultB: number;
  delta: number;
  deltaPercent: number;
  improved: boolean;
}

/**
 * Response from GET /api/results/compare.
 */
export interface ComparisonResponse {
  resultA: TestResultResponse;
  resultB: TestResultResponse;
  deltas: MetricDelta[];
}

/**
 * Filters for the history page.
 */
export interface HistoryFilters {
  status?: string;
  type?: string;
  search?: string;
  dateFrom?: string;
  dateTo?: string;
}

/**
 * Histogram bucket from GET /api/metrics/{id}/histogram.
 */
export interface HistogramBucket {
  range: string;
  count: number;
}

/**
 * Entry for status code distribution chart.
 */
export interface StatusCodeEntry {
  code: string;
  count: number;
  color: string;
}
