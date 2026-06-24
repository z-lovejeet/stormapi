import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { Activity } from 'lucide-react';
import { KpiCard } from '../../components/common/KpiCard';

describe('KpiCard', () => {
  it('renders label and value', () => {
    render(<KpiCard icon={Activity} label="Total Requests" value="1,234" />);
    expect(screen.getByText('Total Requests')).toBeInTheDocument();
    expect(screen.getByText('1,234')).toBeInTheDocument();
  });

  it('renders trend up', () => {
    render(<KpiCard icon={Activity} label="RPS" value="100" trend={12.5} trendDirection="up" />);
    expect(screen.getByText('12.5%')).toBeInTheDocument();
  });

  it('renders trend down', () => {
    render(<KpiCard icon={Activity} label="Errors" value="5" trend={-3.2} trendDirection="down" />);
    expect(screen.getByText('3.2%')).toBeInTheDocument();
  });

  it('renders loading skeleton', () => {
    const { container } = render(<KpiCard icon={Activity} label="Loading" value="" loading />);
    expect(container.querySelector('.loading')).toBeInTheDocument();
  });
});
