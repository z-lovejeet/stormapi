import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { LiveLineChart, type ChartDataPoint } from '../../components/charts/LiveLineChart';

// Mock ResizeObserver for Recharts ResponsiveContainer
class ResizeObserver {
  observe() {}
  unobserve() {}
  disconnect() {}
}
globalThis.ResizeObserver = ResizeObserver;

const sampleData: ChartDataPoint[] = [
  { time: '1', value: 100 },
  { time: '2', value: 150 },
  { time: '3', value: 120 },
  { time: '4', value: 200 },
];

describe('LiveLineChart', () => {
  it('renders "Waiting for data" when data is empty', () => {
    render(<LiveLineChart data={[]} label="Response Time" color="#3b82f6" />);
    expect(screen.getByText('Waiting for data…')).toBeInTheDocument();
  });

  it('renders chart title', () => {
    render(<LiveLineChart data={sampleData} label="Throughput" color="#10b981" />);
    expect(screen.getByText('Throughput')).toBeInTheDocument();
  });

  it('renders current value when provided', () => {
    render(
      <LiveLineChart data={sampleData} label="Error Rate" color="#ef4444" currentValue="2.5" unit="%" />,
    );
    expect(screen.getByText('2.5 %')).toBeInTheDocument();
  });

  it('has correct aria-label', () => {
    render(<LiveLineChart data={sampleData} label="Active Users" color="#8b5cf6" />);
    expect(screen.getByRole('img', { name: 'Active Users chart' })).toBeInTheDocument();
  });
});
