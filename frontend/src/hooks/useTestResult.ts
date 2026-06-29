import { useState, useEffect, useCallback } from 'react';
import type { TestConfigResponse, TestResultResponse } from '../types/test';
import type { MetricSnapshotResponse } from '../types/metrics';
import type { HistogramBucket } from '../types/result';
import { getTest } from '../api/testApi';
import {
  getLatestResult,
  getSnapshots,
  getStatusCodeDistribution,
  getHistogram,
} from '../api/resultApi';

export interface UseTestResultReturn {
  testConfig: TestConfigResponse | null;
  result: TestResultResponse | null;
  snapshots: MetricSnapshotResponse[];
  statusCodes: Record<number, number>;
  histogram: HistogramBucket[];
  loading: boolean;
  error: string | null;
  refresh: () => void;
}

export function useTestResult(testId: number): UseTestResultReturn {
  const [testConfig, setTestConfig] = useState<TestConfigResponse | null>(null);
  const [result, setResult] = useState<TestResultResponse | null>(null);
  const [snapshots, setSnapshots] = useState<MetricSnapshotResponse[]>([]);
  const [statusCodes, setStatusCodes] = useState<Record<number, number>>({});
  const [histogram, setHistogram] = useState<HistogramBucket[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [config, latestResult] = await Promise.all([
        getTest(testId),
        getLatestResult(testId),
      ]);
      setTestConfig(config);
      setResult(latestResult);

      if (latestResult) {
        const [snap, codes, hist] = await Promise.all([
          getSnapshots(latestResult.id),
          getStatusCodeDistribution(latestResult.id),
          getHistogram(latestResult.id),
        ]);
        setSnapshots(snap);
        setStatusCodes(codes);
        setHistogram(hist);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load result');
    } finally {
      setLoading(false);
    }
  }, [testId]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  return { testConfig, result, snapshots, statusCodes, histogram, loading, error, refresh: fetchData };
}
