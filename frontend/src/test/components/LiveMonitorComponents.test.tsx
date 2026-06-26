import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { ProgressBar } from '../../components/live-monitor/ProgressBar';
import { ConnectionStatus } from '../../components/live-monitor/ConnectionStatus';
import { CompletionBanner } from '../../components/live-monitor/CompletionBanner';
import { RequestLogTable } from '../../components/live-monitor/RequestLogTable';
import type { RequestLogEntry } from '../../types/websocket';

describe('ProgressBar', () => {
  it('renders elapsed and total duration', () => {
    render(<ProgressBar elapsedSeconds={30} totalSeconds={120} status="running" />);
    expect(screen.getByText('0:30 / 2:00')).toBeInTheDocument();
  });

  it('renders percentage', () => {
    render(<ProgressBar elapsedSeconds={60} totalSeconds={120} status="running" />);
    expect(screen.getByText('50.0%')).toBeInTheDocument();
  });

  it('shows "Completed" for completed status', () => {
    render(<ProgressBar elapsedSeconds={120} totalSeconds={120} status="completed" />);
    expect(screen.getByText('Completed')).toBeInTheDocument();
  });

  it('has progressbar role with correct attributes', () => {
    render(<ProgressBar elapsedSeconds={25} totalSeconds={100} status="running" />);
    const bar = screen.getByRole('progressbar');
    expect(bar).toHaveAttribute('aria-valuenow', '25');
  });
});

describe('ConnectionStatus', () => {
  it('renders "Connected" state', () => {
    render(<ConnectionStatus state="CONNECTED" />);
    expect(screen.getByText('Connected')).toBeInTheDocument();
  });

  it('renders "Connecting…" state', () => {
    render(<ConnectionStatus state="CONNECTING" />);
    expect(screen.getByText('Connecting…')).toBeInTheDocument();
  });

  it('has status role', () => {
    render(<ConnectionStatus state="ERROR" />);
    expect(screen.getByRole('status')).toBeInTheDocument();
  });
});

describe('CompletionBanner', () => {
  const renderBanner = (status: 'completed' | 'failed' | 'cancelled', visible = true) =>
    render(
      <MemoryRouter>
        <CompletionBanner testId={42} status={status} visible={visible} />
      </MemoryRouter>,
    );

  it('renders completion message when visible', () => {
    renderBanner('completed');
    expect(screen.getByText('🎉 Test Completed!')).toBeInTheDocument();
  });

  it('renders failure message', () => {
    renderBanner('failed');
    expect(screen.getByText('Test Failed')).toBeInTheDocument();
  });

  it('renders cancelled message', () => {
    renderBanner('cancelled');
    expect(screen.getByText('Test Stopped')).toBeInTheDocument();
  });

  it('renders "View Results" button', () => {
    renderBanner('completed');
    expect(screen.getByText('View Results')).toBeInTheDocument();
  });

  it('does not render when not visible', () => {
    renderBanner('completed', false);
    expect(screen.queryByText('🎉 Test Completed!')).not.toBeInTheDocument();
  });
});

describe('RequestLogTable', () => {
  const mockEntries: RequestLogEntry[] = [
    {
      timestamp: '2026-06-26T10:00:01Z',
      url: 'http://api.test.com',
      method: 'GET',
      statusCode: 200,
      responseTimeMs: 45,
      responseSize: 1024,
      errorMessage: null,
      success: true,
    },
    {
      timestamp: '2026-06-26T10:00:02Z',
      url: 'http://api.test.com',
      method: 'POST',
      statusCode: 500,
      responseTimeMs: 120,
      responseSize: 256,
      errorMessage: 'Internal Server Error',
      success: false,
    },
  ];

  it('renders entries', () => {
    render(<RequestLogTable entries={mockEntries} paused={false} onTogglePause={vi.fn()} />);
    expect(screen.getByText('200')).toBeInTheDocument();
    expect(screen.getByText('500')).toBeInTheDocument();
  });

  it('shows count in header', () => {
    render(<RequestLogTable entries={mockEntries} paused={false} onTogglePause={vi.fn()} />);
    expect(screen.getByText('Request Log (2)')).toBeInTheDocument();
  });

  it('shows empty state when no entries', () => {
    render(<RequestLogTable entries={[]} paused={false} onTogglePause={vi.fn()} />);
    expect(screen.getByText('No requests yet…')).toBeInTheDocument();
  });

  it('toggle pause button fires callback', () => {
    const onToggle = vi.fn();
    render(<RequestLogTable entries={mockEntries} paused={false} onTogglePause={onToggle} />);
    fireEvent.click(screen.getByText('Pause'));
    expect(onToggle).toHaveBeenCalledOnce();
  });

  it('shows Resume when paused', () => {
    render(<RequestLogTable entries={mockEntries} paused={true} onTogglePause={vi.fn()} />);
    expect(screen.getByText('Resume')).toBeInTheDocument();
  });
});
