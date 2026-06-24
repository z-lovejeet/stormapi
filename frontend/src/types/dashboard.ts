import type { TestType, TestSummaryResponse } from './test';

/**
 * Dashboard aggregated stats — matches backend DashboardStatsResponse.
 */
export interface DashboardStats {
  totalTests: number;
  totalRuns: number;
  runningTests: number;
  completedTests: number;
  failedTests: number;
  totalRequestsSent: number;
  avgResponseTimeMs: number;
  avgThroughputRps: number;
  avgErrorRate: number;
  recentTests: TestSummaryResponse[];
  testTypeDistribution: Record<TestType, number>;
}
