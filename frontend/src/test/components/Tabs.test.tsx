import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { Tabs } from '../../components/common/Tabs';

const tabs = [
  { id: 'overview', label: 'Overview', content: <div>Overview content</div> },
  { id: 'metrics', label: 'Metrics', content: <div>Metrics content</div> },
  { id: 'logs', label: 'Logs', content: <div>Logs content</div> },
];

describe('Tabs', () => {
  it('renders all tab labels', () => {
    render(<Tabs tabs={tabs} />);
    expect(screen.getByText('Overview')).toBeInTheDocument();
    expect(screen.getByText('Metrics')).toBeInTheDocument();
    expect(screen.getByText('Logs')).toBeInTheDocument();
  });

  it('shows first tab content by default', () => {
    render(<Tabs tabs={tabs} />);
    expect(screen.getByText('Overview content')).toBeInTheDocument();
  });

  it('switches content on click', () => {
    render(<Tabs tabs={tabs} />);
    fireEvent.click(screen.getByText('Metrics'));
    expect(screen.getByText('Metrics content')).toBeInTheDocument();
  });

  it('calls onChange callback', () => {
    const handler = vi.fn();
    render(<Tabs tabs={tabs} onChange={handler} />);
    fireEvent.click(screen.getByText('Logs'));
    expect(handler).toHaveBeenCalledWith('logs');
  });

  it('respects defaultTab', () => {
    render(<Tabs tabs={tabs} defaultTab="metrics" />);
    expect(screen.getByText('Metrics content')).toBeInTheDocument();
  });

  it('sets aria-selected correctly', () => {
    render(<Tabs tabs={tabs} />);
    expect(screen.getByText('Overview').closest('button')).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByText('Metrics').closest('button')).toHaveAttribute('aria-selected', 'false');
  });
});
