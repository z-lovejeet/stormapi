import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { DonutChart } from '../../components/charts/DonutChart';
import { PercentileBarChart } from '../../components/charts/PercentileBarChart';
import { StatusCodeChart } from '../../components/charts/StatusCodeChart';
import { ResponseTimeHistogram } from '../../components/charts/ResponseTimeHistogram';
import { ScalabilityCurve } from '../../components/charts/ScalabilityCurve';

// Mock ResizeObserver for Recharts ResponsiveContainer
class ResizeObserver {
  observe() {}
  unobserve() {}
  disconnect() {}
}
globalThis.ResizeObserver = ResizeObserver;

// ── DonutChart ──────────────────────────────────

describe('DonutChart', () => {
  it('renders empty state when both counts are 0', () => {
    render(<DonutChart successCount={0} failureCount={0} />);
    expect(screen.getByText(/no.*data/i)).toBeInTheDocument();
  });

  it('renders title', () => {
    render(<DonutChart successCount={950} failureCount={50} />);
    expect(screen.getByText('Success Rate')).toBeInTheDocument();
  });

  it('has correct aria-label with data', () => {
    render(<DonutChart successCount={100} failureCount={5} />);
    expect(screen.getByRole('img', { name: /success rate/i })).toBeInTheDocument();
  });
});

// ── PercentileBarChart ──────────────────────────

describe('PercentileBarChart', () => {
  it('renders empty state when all percentiles are 0', () => {
    render(<PercentileBarChart p50={0} p75={0} p90={0} p95={0} p99={0} />);
    expect(screen.getByText('No percentile data available')).toBeInTheDocument();
  });

  it('renders title with data', () => {
    render(<PercentileBarChart p50={50} p75={75} p90={120} p95={200} p99={500} />);
    expect(screen.getByText('Response Time Percentiles')).toBeInTheDocument();
  });

  it('has correct aria-label', () => {
    render(<PercentileBarChart p50={50} p75={75} p90={120} p95={200} p99={500} />);
    expect(screen.getByRole('img', { name: /percentile/i })).toBeInTheDocument();
  });
});

// ── StatusCodeChart ─────────────────────────────

describe('StatusCodeChart', () => {
  it('renders empty state with empty data', () => {
    render(<StatusCodeChart data={{}} />);
    expect(screen.getByText('No status code data available')).toBeInTheDocument();
  });

  it('renders title with data', () => {
    render(<StatusCodeChart data={{ 200: 900, 404: 50, 500: 10 }} />);
    expect(screen.getByText('Status Code Distribution')).toBeInTheDocument();
  });

  it('has correct aria-label', () => {
    render(<StatusCodeChart data={{ 200: 100 }} />);
    expect(screen.getByRole('img', { name: /status code/i })).toBeInTheDocument();
  });
});

// ── ResponseTimeHistogram ───────────────────────

describe('ResponseTimeHistogram', () => {
  it('renders empty state with no data', () => {
    render(<ResponseTimeHistogram data={[]} />);
    expect(screen.getByText('No data available')).toBeInTheDocument();
  });

  it('renders title with data', () => {
    render(<ResponseTimeHistogram data={[{ range: '0-50', count: 100 }, { range: '50-100', count: 50 }]} />);
    expect(screen.getByText('Response Time Distribution')).toBeInTheDocument();
  });

  it('has correct aria-label', () => {
    render(<ResponseTimeHistogram data={[{ range: '0-50', count: 10 }]} />);
    expect(screen.getByRole('img', { name: /histogram/i })).toBeInTheDocument();
  });
});

// ── ScalabilityCurve ────────────────────────────

describe('ScalabilityCurve', () => {
  it('renders empty state with no data', () => {
    render(<ScalabilityCurve snapshots={[]} />);
    expect(screen.getByText('No data available')).toBeInTheDocument();
  });

  it('renders title with data', () => {
    render(
      <ScalabilityCurve
        snapshots={[
          { activeUsers: 10, requestsPerSecond: 50, avgResponseTimeMs: 100 },
          { activeUsers: 50, requestsPerSecond: 200, avgResponseTimeMs: 150 },
        ]}
      />,
    );
    expect(screen.getByText('Scalability Curve')).toBeInTheDocument();
  });

  it('has correct aria-label', () => {
    render(
      <ScalabilityCurve
        snapshots={[{ activeUsers: 10, requestsPerSecond: 50, avgResponseTimeMs: 100 }]}
      />,
    );
    expect(screen.getByRole('img', { name: /scalability/i })).toBeInTheDocument();
  });
});
