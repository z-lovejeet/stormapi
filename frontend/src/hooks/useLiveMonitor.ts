import { useState, useEffect, useCallback, useRef } from 'react';
import { useWebSocket } from './useWebSocket';
import { getTest } from '../api/testApi';
import type { TestConfigResponse } from '../types/test';
import type {
  LiveMetricsMessage,
  RequestLogMessage,
  TestEventMessage,
  WebSocketConnectionState,
  RequestLogEntry,
} from '../types/websocket';
import type { ChartDataPoint } from '../components/charts/LiveLineChart';

const MAX_CHART_POINTS = 60;

interface CompletionState {
  completed: boolean;
  status: 'completed' | 'failed' | 'cancelled';
  resultId?: number;
}

interface KpiValues {
  totalRequests: number;
  throughputRps: number;
  avgResponseTimeMs: number;
  errorRate: number;
}

interface ChartDataArrays {
  responseTime: ChartDataPoint[];
  throughput: ChartDataPoint[];
  errorRate: ChartDataPoint[];
  activeUsers: ChartDataPoint[];
}

export interface UseLiveMonitorReturn {
  testConfig: TestConfigResponse | null;
  configLoading: boolean;
  configError: string | null;
  connectionState: WebSocketConnectionState;
  kpis: KpiValues;
  chartData: ChartDataArrays;
  logEntries: RequestLogEntry[];
  elapsedSeconds: number;
  completionState: CompletionState;
  disconnect: () => void;
}

function appendPoint(arr: ChartDataPoint[], time: string, value: number): ChartDataPoint[] {
  const next = [...arr, { time, value }];
  return next.length > MAX_CHART_POINTS ? next.slice(-MAX_CHART_POINTS) : next;
}

export function useLiveMonitor(testId: number): UseLiveMonitorReturn {
  // Test config from REST
  const [testConfig, setTestConfig] = useState<TestConfigResponse | null>(null);
  const [configLoading, setConfigLoading] = useState(true);
  const [configError, setConfigError] = useState<string | null>(null);

  // KPI values
  const [kpis, setKpis] = useState<KpiValues>({
    totalRequests: 0,
    throughputRps: 0,
    avgResponseTimeMs: 0,
    errorRate: 0,
  });

  // Chart data — use state for reactivity with bounded arrays
  const [chartData, setChartData] = useState<ChartDataArrays>({
    responseTime: [],
    throughput: [],
    errorRate: [],
    activeUsers: [],
  });

  // Elapsed time
  const [elapsedSeconds, setElapsedSeconds] = useState(0);
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const tickRef = useRef(0);

  // Completion
  const [completionState, setCompletionState] = useState<CompletionState>({
    completed: false,
    status: 'completed',
  });

  // Fetch test config on mount
  useEffect(() => {
    let cancelled = false;
    setConfigLoading(true);
    setConfigError(null);

    getTest(testId)
      .then((config) => {
        if (!cancelled) {
          setTestConfig(config);
          // If test already completed, mark it
          if (['COMPLETED', 'FAILED', 'CANCELLED'].includes(config.status)) {
            setCompletionState({
              completed: true,
              status: config.status.toLowerCase() as CompletionState['status'],
            });
          }
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setConfigError(err instanceof Error ? err.message : 'Failed to load test');
        }
      })
      .finally(() => {
        if (!cancelled) setConfigLoading(false);
      });

    return () => { cancelled = true; };
  }, [testId]);

  // Elapsed timer
  useEffect(() => {
    if (completionState.completed) {
      if (timerRef.current) clearInterval(timerRef.current);
      return;
    }

    timerRef.current = setInterval(() => {
      setElapsedSeconds((s) => s + 1);
    }, 1000);

    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, [completionState.completed]);

  // WebSocket callbacks
  const onMetrics = useCallback((metrics: LiveMetricsMessage) => {
    tickRef.current += 1;
    const t = String(tickRef.current);

    setKpis({
      totalRequests: metrics.totalRequests,
      throughputRps: metrics.throughputRps,
      avgResponseTimeMs: metrics.avgResponseTimeMs,
      errorRate: metrics.errorRate,
    });

    setChartData((prev) => ({
      responseTime: appendPoint(prev.responseTime, t, metrics.avgResponseTimeMs),
      throughput: appendPoint(prev.throughput, t, metrics.throughputRps),
      errorRate: appendPoint(prev.errorRate, t, metrics.errorRate),
      activeUsers: appendPoint(prev.activeUsers, t, metrics.activeUsers),
    }));
  }, []);

  const onLogBatch = useCallback((_logBatch: RequestLogMessage) => {
    // Logs are already managed by useWebSocket's latestLogs state
  }, []);

  const onEvent = useCallback((event: TestEventMessage) => {
    if (event.eventType === 'TEST_PROGRESS') {
      const elapsed = event.metadata?.elapsedSeconds;
      if (typeof elapsed === 'number') {
        setElapsedSeconds(elapsed);
      }
    }

    if (
      event.eventType === 'TEST_COMPLETED' ||
      event.eventType === 'TEST_FAILED' ||
      event.eventType === 'TEST_CANCELLED' ||
      event.eventType === 'TEST_STOPPED'
    ) {
      const statusMap: Record<string, CompletionState['status']> = {
        TEST_COMPLETED: 'completed',
        TEST_FAILED: 'failed',
        TEST_CANCELLED: 'cancelled',
        TEST_STOPPED: 'cancelled',
      };
      setCompletionState({
        completed: true,
        status: statusMap[event.eventType] ?? 'completed',
        resultId: event.metadata?.resultId as number | undefined,
      });

      // Snap progress to total on completion
      if (event.eventType === 'TEST_COMPLETED') {
        setElapsedSeconds((s) => {
          // Use testConfig duration if available
          return s; // Timer will be stopped by completionState effect
        });
      }
    }
  }, []);

  const wsEnabled = !completionState.completed && !configLoading && !configError;

  const { connectionState, latestLogs, disconnect } = useWebSocket({
    testId,
    enabled: wsEnabled,
    onMetrics,
    onLogBatch,
    onEvent,
  });

  return {
    testConfig,
    configLoading,
    configError,
    connectionState,
    kpis,
    chartData,
    logEntries: latestLogs,
    elapsedSeconds,
    completionState,
    disconnect,
  };
}
