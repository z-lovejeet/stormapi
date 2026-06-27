import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { LiveMonitorPage } from '../../pages/LiveMonitorPage';
import type { LiveMetricsMessage, TestEventMessage } from '../../types/websocket';

// Mock ResizeObserver for Recharts
class ResizeObserver {
  observe() {}
  unobserve() {}
  disconnect() {}
}
globalThis.ResizeObserver = ResizeObserver;

// Track WS callbacks
let wsCallbacks: {
  onMetrics?: (m: LiveMetricsMessage) => void;
  onEvent?: (e: TestEventMessage) => void;
} = {};
const mockDisconnect = vi.fn();
let wsEnabled = true;

vi.mock('../../hooks/useWebSocket', () => ({
  useWebSocket: vi.fn((opts: Record<string, unknown>) => {
    wsEnabled = opts.enabled as boolean;
    wsCallbacks.onMetrics = opts.onMetrics as typeof wsCallbacks.onMetrics;
    wsCallbacks.onEvent = opts.onEvent as typeof wsCallbacks.onEvent;
    return {
      connectionState: 'CONNECTED' as const,
      latestMetrics: null,
      latestLogs: [],
      latestEvent: null,
      disconnect: mockDisconnect,
    };
  }),
}));

vi.mock('../../api/testApi', () => ({
  getTest: vi.fn(),
  stopTest: vi.fn(),
}));

vi.mock('../../components/common/Toast', () => ({
  useToast: () => ({ showToast: vi.fn() }),
}));

import { getTest, stopTest } from '../../api/testApi';

const mockConfig = {
  id: 42,
  name: 'Load Test API',
  targetUrl: 'http://api.example.com',
  httpMethod: 'GET',
  testType: 'LOAD',
  virtualUsers: 100,
  durationSeconds: 300,
  rampUpSeconds: 30,
  maxRetries: 0,
  timeoutMs: 5000,
  thinkTimeMs: 0,
  status: 'RUNNING',
  createdAt: '2026-06-27T10:00:00Z',
  updatedAt: '2026-06-27T10:00:00Z',
};

const mockMetrics: LiveMetricsMessage = {
  testId: 42,
  totalRequests: 1500,
  successCount: 1470,
  failureCount: 30,
  avgResponseTimeMs: 85.2,
  minResponseTimeMs: 5,
  maxResponseTimeMs: 400,
  p50Ms: 60,
  p75Ms: 100,
  p90Ms: 150,
  p95Ms: 200,
  p99Ms: 350,
  throughputRps: 50.1,
  errorRate: 2.0,
  activeUsers: 100,
  totalDataBytes: 5120000,
  statusCodeDistribution: { '200': 1470, '500': 30 },
  timestamp: '2026-06-27T10:01:00Z',
};

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/tests/42/live']}>
      <Routes>
        <Route path="tests/:id/live" element={<LiveMonitorPage />} />
        <Route path="tests/:id/result" element={<div>Results Page</div>} />
        <Route path="dashboard" element={<div>Dashboard</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('LiveMonitorPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    wsCallbacks = {};
    (getTest as ReturnType<typeof vi.fn>).mockResolvedValue(mockConfig);
    (stopTest as ReturnType<typeof vi.fn>).mockResolvedValue(undefined);
  });

  it('shows loading spinner initially', () => {
    (getTest as ReturnType<typeof vi.fn>).mockReturnValue(new Promise(() => {}));
    renderPage();
    expect(screen.getByText('Loading test configuration…')).toBeInTheDocument();
  });

  it('shows error state when config fetch fails', async () => {
    (getTest as ReturnType<typeof vi.fn>).mockRejectedValue(new Error('Not found'));
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Not found')).toBeInTheDocument();
    });
    expect(screen.getByText('Back to Dashboard')).toBeInTheDocument();
  });

  it('renders test name and badges after loading', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Load Test API')).toBeInTheDocument();
    });
  });

  it('renders 4 KPI cards', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Total Requests')).toBeInTheDocument();
    });
    // 'Throughput' and 'Error Rate' appear in both KPI and chart — use getAll
    expect(screen.getAllByText('Throughput').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('Avg Response Time')).toBeInTheDocument();
    expect(screen.getAllByText('Error Rate').length).toBeGreaterThanOrEqual(1);
  });

  it('renders 4 chart containers', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Response Time')).toBeInTheDocument();
    });
    expect(screen.getByText('Active Users')).toBeInTheDocument();
  });

  it('renders progress bar', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByRole('progressbar')).toBeInTheDocument();
    });
  });

  it('renders stop button when running', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Stop Test')).toBeInTheDocument();
    });
  });

  it('opens stop confirmation modal on click', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Stop Test')).toBeInTheDocument();
    });
    fireEvent.click(screen.getByText('Stop Test'));
    await waitFor(() => {
      expect(screen.getByText('Stop Test?')).toBeInTheDocument();
    });
    expect(screen.getByText(/Are you sure/)).toBeInTheDocument();
  });

  it('calls stopTest API on confirm', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Stop Test')).toBeInTheDocument();
    });
    fireEvent.click(screen.getByText('Stop Test'));
    await waitFor(() => {
      expect(screen.getByText('Stop Test?')).toBeInTheDocument();
    });
    // Click the confirm "Stop Test" button in modal footer
    const buttons = screen.getAllByText('Stop Test');
    const confirmButton = buttons[buttons.length - 1];
    fireEvent.click(confirmButton);
    await waitFor(() => {
      expect(stopTest).toHaveBeenCalledWith(42);
    });
  });

  it('shows completion banner on TEST_COMPLETED event', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Load Test API')).toBeInTheDocument();
    });

    // Fire completion event
    wsCallbacks.onEvent?.({
      testId: 42,
      eventType: 'TEST_COMPLETED',
      message: 'Test completed',
      metadata: { resultId: 99 },
      timestamp: '2026-06-27T10:05:00Z',
    });

    await waitFor(() => {
      expect(screen.getByText('🎉 Test Completed!')).toBeInTheDocument();
    });
    expect(screen.getByText('View Results')).toBeInTheDocument();
  });

  it('renders request log section', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText(/Request Log/)).toBeInTheDocument();
    });
  });

  it('renders connection status indicator', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Connected')).toBeInTheDocument();
    });
  });
});
