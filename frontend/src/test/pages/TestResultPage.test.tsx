import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { TestResultPage } from '../../pages/TestResultPage';

// Mock ResizeObserver
class ResizeObserver {
  observe() {}
  unobserve() {}
  disconnect() {}
}
globalThis.ResizeObserver = ResizeObserver;

// Mock the hook
const mockUseTestResult = vi.fn();
vi.mock('../../hooks/useTestResult', () => ({
  useTestResult: (...args: unknown[]) => mockUseTestResult(...args),
}));

function renderWithRouter(testId = '1') {
  return render(
    <MemoryRouter initialEntries={[`/tests/${testId}/result`]}>
      <Routes>
        <Route path="tests/:id/result" element={<TestResultPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('TestResultPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders loading state', () => {
    mockUseTestResult.mockReturnValue({
      testConfig: null,
      result: null,
      snapshots: [],
      statusCodes: {},
      histogram: [],
      loading: true,
      error: null,
      refresh: vi.fn(),
    });
    renderWithRouter();
    expect(screen.getByText('Loading test results…')).toBeInTheDocument();
  });

  it('renders error state', () => {
    mockUseTestResult.mockReturnValue({
      testConfig: null,
      result: null,
      snapshots: [],
      statusCodes: {},
      histogram: [],
      loading: false,
      error: 'Network error',
      refresh: vi.fn(),
    });
    renderWithRouter();
    expect(screen.getByText(/failed to load results/i)).toBeInTheDocument();
  });

  it('renders empty state when no result', () => {
    mockUseTestResult.mockReturnValue({
      testConfig: null,
      result: null,
      snapshots: [],
      statusCodes: {},
      histogram: [],
      loading: false,
      error: null,
      refresh: vi.fn(),
    });
    renderWithRouter();
    expect(screen.getByText(/no results yet/i)).toBeInTheDocument();
  });

  it('renders result page with data', () => {
    mockUseTestResult.mockReturnValue({
      testConfig: { id: 1, name: 'Load Test', testType: 'LOAD', virtualUsers: 100 },
      result: {
        id: 1, testConfigId: 1, status: 'COMPLETED',
        totalRequests: 5000, successCount: 4800, failureCount: 200,
        avgResponseTimeMs: 150, minResponseTimeMs: 10, maxResponseTimeMs: 3000,
        p50Ms: 100, p75Ms: 150, p90Ms: 250, p95Ms: 400, p99Ms: 1500,
        throughputRps: 83.3, errorRate: 4.0, totalDataBytes: 10485760,
        startedAt: '2025-01-15T10:00:00Z', completedAt: '2025-01-15T10:01:00Z',
        durationMs: 60000, createdAt: '2025-01-15T10:00:00Z',
      },
      snapshots: [],
      statusCodes: { 200: 4800, 500: 200 },
      histogram: [{ range: '0-50', count: 100 }],
      loading: false,
      error: null,
      refresh: vi.fn(),
    });
    renderWithRouter();
    expect(screen.getByText('Test Results')).toBeInTheDocument();
    expect(screen.getByText('Performance Timeline')).toBeInTheDocument();
    expect(screen.getAllByText(/detailed metrics/i).length).toBeGreaterThan(0);
  });
});
