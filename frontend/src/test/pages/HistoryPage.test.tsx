import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { HistoryPage } from '../../pages/HistoryPage';

// Mock ResizeObserver
class ResizeObserver {
  observe() {}
  unobserve() {}
  disconnect() {}
}
globalThis.ResizeObserver = ResizeObserver;

// Mock the hook
const mockUseHistory = vi.fn();
vi.mock('../../hooks/useHistory', () => ({
  useHistory: () => mockUseHistory(),
}));

function renderPage() {
  return render(
    <MemoryRouter>
      <HistoryPage />
    </MemoryRouter>,
  );
}

const baseReturn = {
  tests: [],
  loading: false,
  error: null,
  page: 0,
  totalPages: 1,
  totalElements: 0,
  filters: {},
  setFilters: vi.fn(),
  sortField: 'createdAt',
  sortDir: 'desc' as const,
  setSort: vi.fn(),
  setPage: vi.fn(),
  compareSelection: [],
  toggleCompare: vi.fn(),
  clearCompare: vi.fn(),
  refresh: vi.fn(),
};

describe('HistoryPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders loading state', () => {
    mockUseHistory.mockReturnValue({ ...baseReturn, loading: true });
    renderPage();
    expect(screen.getByText('Loading test history…')).toBeInTheDocument();
  });

  it('renders header and filters', () => {
    mockUseHistory.mockReturnValue({ ...baseReturn, totalElements: 5 });
    renderPage();
    expect(screen.getByText('Test History')).toBeInTheDocument();
    expect(screen.getByText('5 tests')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Search tests…')).toBeInTheDocument();
  });

  it('renders empty table message', () => {
    mockUseHistory.mockReturnValue(baseReturn);
    renderPage();
    expect(screen.getByText(/no tests found/i)).toBeInTheDocument();
  });

  it('renders test rows', () => {
    mockUseHistory.mockReturnValue({
      ...baseReturn,
      totalElements: 1,
      tests: [{
        id: 1,
        name: 'My Load Test',
        targetUrl: 'http://example.com',
        testType: 'LOAD',
        status: 'COMPLETED',
        virtualUsers: 50,
        durationSeconds: 60,
        lastAvgResponseTimeMs: 150,
        lastErrorRate: 2.5,
        totalRuns: 3,
        createdAt: '2025-01-15T10:00:00Z',
      }],
    });
    renderPage();
    expect(screen.getByText('My Load Test')).toBeInTheDocument();
  });

  it('shows compare bar when items selected', () => {
    mockUseHistory.mockReturnValue({
      ...baseReturn,
      compareSelection: [1, 2],
    });
    renderPage();
    expect(screen.getByText('2/2 selected for comparison')).toBeInTheDocument();
    expect(screen.getByText(/compare/i)).toBeInTheDocument();
  });

  it('shows pagination for multiple pages', () => {
    mockUseHistory.mockReturnValue({
      ...baseReturn,
      totalPages: 3,
      page: 1,
    });
    renderPage();
    expect(screen.getByText('Page 2 of 3')).toBeInTheDocument();
    expect(screen.getByText('Previous')).toBeInTheDocument();
    expect(screen.getByText('Next')).toBeInTheDocument();
  });
});
