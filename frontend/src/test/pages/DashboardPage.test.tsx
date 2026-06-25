import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import type { ReactNode } from 'react';
import { ThemeProvider } from '../../context/ThemeContext';
import { ToastProvider } from '../../components/common/Toast';
import { DashboardPage } from '../../pages/DashboardPage';
import * as dashboardApi from '../../api/dashboardApi';
import * as testApi from '../../api/testApi';
import type { DashboardStats } from '../../types/dashboard';
import { TestType, TestStatus, HttpMethod } from '../../types/test';
import type { TestSummaryResponse } from '../../types/test';

vi.mock('../../api/dashboardApi');
vi.mock('../../api/testApi');

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...actual, useNavigate: () => mockNavigate };
});

const recentTest: TestSummaryResponse = {
  id: 1,
  name: 'Load Test Homepage',
  targetUrl: 'https://api.example.com/health',
  testType: TestType.LOAD,
  status: TestStatus.COMPLETED,
  virtualUsers: 100,
  durationSeconds: 60,
  lastRunAt: '2026-06-20T10:00:00Z',
  lastAvgResponseTimeMs: 45.5,
  lastErrorRate: 1.2,
  totalRuns: 3,
  createdAt: '2026-06-20T09:00:00Z',
};

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
  recentTests: [recentTest],
  testTypeDistribution: { [TestType.LOAD]: 5 } as DashboardStats['testTypeDistribution'],
};

const emptyStats: DashboardStats = {
  ...mockStats,
  totalTests: 0,
  totalRequestsSent: 0,
  avgResponseTimeMs: 0,
  avgThroughputRps: 0,
  recentTests: [],
};

function wrapper({ children }: { children: ReactNode }) {
  return (
    <MemoryRouter>
      <ThemeProvider>
        <ToastProvider>{children}</ToastProvider>
      </ThemeProvider>
    </MemoryRouter>
  );
}

describe('DashboardPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders 4 KPI cards with formatted values', async () => {
    vi.mocked(dashboardApi.getStats).mockResolvedValue(mockStats);
    render(<DashboardPage />, { wrapper });

    await waitFor(() => {
      expect(screen.getByText('Total Tests')).toBeInTheDocument();
    });
    expect(screen.getByText('Total Requests')).toBeInTheDocument();
    expect(screen.getByText('Avg Response Time')).toBeInTheDocument();
    expect(screen.getByText('Avg Throughput')).toBeInTheDocument();
  });

  it('renders loading spinner while fetching', () => {
    vi.mocked(dashboardApi.getStats).mockReturnValue(new Promise(() => {}));
    render(<DashboardPage />, { wrapper });
    const statusElements = screen.getAllByRole('status');
    expect(statusElements.length).toBeGreaterThanOrEqual(1);
  });

  it('renders empty state when totalTests is 0', async () => {
    vi.mocked(dashboardApi.getStats).mockResolvedValue(emptyStats);
    render(<DashboardPage />, { wrapper });

    await waitFor(() => {
      expect(screen.getByText('No Tests Yet')).toBeInTheDocument();
    });
    expect(screen.getByText('Create Your First Test')).toBeInTheDocument();
  });

  it('renders error banner with retry button', async () => {
    vi.mocked(dashboardApi.getStats).mockRejectedValue(new Error('Server down'));
    render(<DashboardPage />, { wrapper });

    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeInTheDocument();
    });
    expect(screen.getByText(/Server down/)).toBeInTheDocument();
    expect(screen.getByText('Retry')).toBeInTheDocument();
  });

  it('renders recent tests in table', async () => {
    vi.mocked(dashboardApi.getStats).mockResolvedValue(mockStats);
    render(<DashboardPage />, { wrapper });

    await waitFor(() => {
      expect(screen.getByText('Load Test Homepage')).toBeInTheDocument();
    });
    expect(screen.getByText('Completed')).toBeInTheDocument();
  });

  it('navigates to test result on row click', async () => {
    vi.mocked(dashboardApi.getStats).mockResolvedValue(mockStats);
    render(<DashboardPage />, { wrapper });

    await waitFor(() => {
      expect(screen.getByText('Load Test Homepage')).toBeInTheDocument();
    });
    fireEvent.click(screen.getByText('Load Test Homepage'));
    expect(mockNavigate).toHaveBeenCalledWith('/tests/1/result');
  });

  it('delete button calls deleteTest API and refreshes', async () => {
    vi.mocked(dashboardApi.getStats).mockResolvedValue(mockStats);
    vi.mocked(testApi.deleteTest).mockResolvedValue(undefined);
    vi.spyOn(window, 'confirm').mockReturnValue(true);

    render(<DashboardPage />, { wrapper });

    await waitFor(() => {
      expect(screen.getByLabelText('Delete test Load Test Homepage')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByLabelText('Delete test Load Test Homepage'));

    await waitFor(() => {
      expect(testApi.deleteTest).toHaveBeenCalledWith(1);
    });
    // getStats called on mount + refresh after delete
    expect(dashboardApi.getStats).toHaveBeenCalledTimes(2);
  });

  it('rerun button calls rerunTest API and refreshes', async () => {
    vi.mocked(dashboardApi.getStats).mockResolvedValue(mockStats);
    vi.mocked(testApi.rerunTest).mockResolvedValue({} as any);

    render(<DashboardPage />, { wrapper });

    await waitFor(() => {
      expect(screen.getByLabelText('Re-run test Load Test Homepage')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByLabelText('Re-run test Load Test Homepage'));

    await waitFor(() => {
      expect(testApi.rerunTest).toHaveBeenCalledWith(1);
    });
    expect(dashboardApi.getStats).toHaveBeenCalledTimes(2);
  });
});
