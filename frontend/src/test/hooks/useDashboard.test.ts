import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor, act } from '@testing-library/react';
import { useDashboard } from '../../hooks/useDashboard';
import * as dashboardApi from '../../api/dashboardApi';
import type { DashboardStats } from '../../types/dashboard';

vi.mock('../../api/dashboardApi');

const mockStats: DashboardStats = {
  totalTests: 10,
  totalRuns: 25,
  runningTests: 2,
  completedTests: 20,
  failedTests: 3,
  totalRequestsSent: 50000,
  avgResponseTimeMs: 45.5,
  avgThroughputRps: 100.0,
  avgErrorRate: 2.5,
  recentTests: [],
  testTypeDistribution: {} as DashboardStats['testTypeDistribution'],
};

describe('useDashboard', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('returns loading=true initially', () => {
    vi.mocked(dashboardApi.getStats).mockReturnValue(new Promise(() => {}));
    const { result } = renderHook(() => useDashboard());
    expect(result.current.loading).toBe(true);
    expect(result.current.stats).toBeNull();
  });

  it('sets stats after successful fetch', async () => {
    vi.mocked(dashboardApi.getStats).mockResolvedValue(mockStats);
    const { result } = renderHook(() => useDashboard());

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });
    expect(result.current.stats).toEqual(mockStats);
    expect(result.current.error).toBeNull();
  });

  it('sets error on API failure', async () => {
    vi.mocked(dashboardApi.getStats).mockRejectedValue(new Error('Network error'));
    const { result } = renderHook(() => useDashboard());

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });
    expect(result.current.error).toBe('Network error');
    expect(result.current.stats).toBeNull();
  });

  it('refresh re-fetches data', async () => {
    vi.mocked(dashboardApi.getStats).mockResolvedValue(mockStats);
    const { result } = renderHook(() => useDashboard());

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });
    expect(dashboardApi.getStats).toHaveBeenCalledTimes(1);

    await act(async () => {
      result.current.refresh();
    });

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });
    expect(dashboardApi.getStats).toHaveBeenCalledTimes(2);
  });

  it('error clears on successful retry', async () => {
    vi.mocked(dashboardApi.getStats).mockRejectedValueOnce(new Error('Fail'));
    const { result } = renderHook(() => useDashboard());

    await waitFor(() => {
      expect(result.current.error).toBe('Fail');
    });

    vi.mocked(dashboardApi.getStats).mockResolvedValue(mockStats);
    await act(async () => {
      result.current.refresh();
    });

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });
    expect(result.current.error).toBeNull();
    expect(result.current.stats).toEqual(mockStats);
  });
});
