import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { ResultSummaryCards } from '../../components/results/ResultSummaryCards';
import { MetricsDetailTable } from '../../components/results/MetricsDetailTable';
import type { TestResultResponse } from '../../types/test';
import { TestStatus } from '../../types/test';

// Mock ResizeObserver
class ResizeObserver {
  observe() {}
  unobserve() {}
  disconnect() {}
}
globalThis.ResizeObserver = ResizeObserver;

const mockResult: TestResultResponse = {
  id: 1,
  testConfigId: 1,
  status: TestStatus.COMPLETED,
  totalRequests: 1000,
  successCount: 950,
  failureCount: 50,
  avgResponseTimeMs: 125.5,
  minResponseTimeMs: 10,
  maxResponseTimeMs: 3500,
  p50Ms: 80,
  p75Ms: 120,
  p90Ms: 200,
  p95Ms: 350,
  p99Ms: 1200,
  throughputRps: 42.5,
  errorRate: 5.0,
  totalDataBytes: 5242880,
  startedAt: '2025-01-15T10:00:00Z',
  completedAt: '2025-01-15T10:05:00Z',
  durationMs: 300000,
  createdAt: '2025-01-15T10:00:00Z',
};

// ── ResultSummaryCards ──────────────────────────

describe('ResultSummaryCards', () => {
  it('renders KPI values', () => {
    render(<ResultSummaryCards result={mockResult} />);
    // Should show formatted total requests
    expect(screen.getByText('1,000')).toBeInTheDocument();
  });

  it('renders success rate', () => {
    render(<ResultSummaryCards result={mockResult} />);
    expect(screen.getByText('95.0%')).toBeInTheDocument();
  });
});

// ── MetricsDetailTable ──────────────────────────

describe('MetricsDetailTable', () => {
  it('renders table title', () => {
    render(<MetricsDetailTable result={mockResult} />);
    expect(screen.getByText(/detailed metrics/i)).toBeInTheDocument();
  });

  it('renders latency section', () => {
    render(<MetricsDetailTable result={mockResult} />);
    expect(screen.getByText(/latency/i)).toBeInTheDocument();
  });

  it('renders count values', () => {
    render(<MetricsDetailTable result={mockResult} />);
    expect(screen.getByText('1,000')).toBeInTheDocument();
  });
});
