import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import type { ReactNode } from 'react';
import { ThemeProvider } from '../../context/ThemeContext';
import { ToastProvider } from '../../components/common/Toast';
import { TestBuilderPage } from '../../pages/TestBuilderPage';
import * as testApi from '../../api/testApi';

vi.mock('../../api/testApi');

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...actual, useNavigate: () => mockNavigate };
});

function renderWithRouter(ui: ReactNode, initialEntry = '/tests/new') {
  return render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <ThemeProvider>
        <ToastProvider>{ui}</ToastProvider>
      </ThemeProvider>
    </MemoryRouter>,
  );
}

describe('TestBuilderPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders Step 1 on mount with Target heading', () => {
    renderWithRouter(<TestBuilderPage />);
    expect(screen.getByText('Create Test')).toBeInTheDocument();
    expect(screen.getByText('Test Name')).toBeInTheDocument();
    expect(screen.getByText('Target URL')).toBeInTheDocument();
  });

  it('blocks Next if name is empty', async () => {
    renderWithRouter(<TestBuilderPage />);
    const nextBtn = screen.getByText('Next');
    fireEvent.click(nextBtn);
    // Should stay on step 1 — name error shown
    await waitFor(() => {
      expect(screen.getByText('Test Name')).toBeInTheDocument();
    });
  });

  it('advances to Step 2 when step 1 is valid', async () => {
    renderWithRouter(<TestBuilderPage />);
    // Fill required fields
    const nameInput = screen.getByLabelText(/Test Name/i);
    fireEvent.change(nameInput, { target: { value: 'My Test' } });
    const urlInput = screen.getByLabelText(/Target URL/i);
    fireEvent.change(urlInput, { target: { value: 'https://api.example.com' } });

    fireEvent.click(screen.getByText('Next'));
    await waitFor(() => {
      expect(screen.getByText('Load Test')).toBeInTheDocument();
      expect(screen.getByText('Stress Test')).toBeInTheDocument();
    });
  });

  it('renders 6 type cards on Step 2', async () => {
    renderWithRouter(<TestBuilderPage />);
    const nameInput = screen.getByLabelText(/Test Name/i);
    fireEvent.change(nameInput, { target: { value: 'Test' } });
    const urlInput = screen.getByLabelText(/Target URL/i);
    fireEvent.change(urlInput, { target: { value: 'https://example.com' } });
    fireEvent.click(screen.getByText('Next'));

    await waitFor(() => {
      expect(screen.getByText('Spike Test')).toBeInTheDocument();
      expect(screen.getByText('Soak Test')).toBeInTheDocument();
      expect(screen.getByText('Breakpoint Test')).toBeInTheDocument();
      expect(screen.getByText('Scalability Test')).toBeInTheDocument();
    });
  });

  it('navigates back without validation', async () => {
    renderWithRouter(<TestBuilderPage />);
    const nameInput = screen.getByLabelText(/Test Name/i);
    fireEvent.change(nameInput, { target: { value: 'Test' } });
    const urlInput = screen.getByLabelText(/Target URL/i);
    fireEvent.change(urlInput, { target: { value: 'https://example.com' } });
    fireEvent.click(screen.getByText('Next'));

    await waitFor(() => {
      expect(screen.getByText('Load Test')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('Back'));
    await waitFor(() => {
      expect(screen.getByText('Test Name')).toBeInTheDocument();
    });
  });

  it('shows type-specific fields on Step 3', async () => {
    renderWithRouter(<TestBuilderPage />);
    // Step 1
    fireEvent.change(screen.getByLabelText(/Test Name/i), { target: { value: 'Test' } });
    fireEvent.change(screen.getByLabelText(/Target URL/i), { target: { value: 'https://example.com' } });
    fireEvent.click(screen.getByText('Next'));

    // Step 2 — select Spike
    await waitFor(() => {
      expect(screen.getByText('Spike Test')).toBeInTheDocument();
    });
    fireEvent.click(screen.getByText('Spike Test'));
    fireEvent.click(screen.getByText('Next'));

    // Step 3 — should show spikeUsers field
    await waitFor(() => {
      expect(screen.getByText('Spike Users')).toBeInTheDocument();
    });
  });

  it('shows review on Step 4 with test config values', async () => {
    renderWithRouter(<TestBuilderPage />);
    // Step 1
    fireEvent.change(screen.getByLabelText(/Test Name/i), { target: { value: 'Load Homepage' } });
    fireEvent.change(screen.getByLabelText(/Target URL/i), { target: { value: 'https://example.com' } });
    fireEvent.click(screen.getByText('Next'));

    // Step 2 — select Load
    await waitFor(() => {
      expect(screen.getByText('Load Test')).toBeInTheDocument();
    });
    fireEvent.click(screen.getByText('Load Test'));
    fireEvent.click(screen.getByText('Next'));

    // Step 3 — accept defaults
    await waitFor(() => {
      expect(screen.getByText('Virtual Users')).toBeInTheDocument();
    });
    fireEvent.click(screen.getByText('Next'));

    // Step 4 — review
    await waitFor(() => {
      expect(screen.getByText('Start Test')).toBeInTheDocument();
      expect(screen.getByText('Load Homepage')).toBeInTheDocument();
    });
  });

  it('submits and navigates to live monitor on success', async () => {
    vi.mocked(testApi.createTest).mockResolvedValue({ id: 42 } as any);
    renderWithRouter(<TestBuilderPage />);

    // Fill step 1
    fireEvent.change(screen.getByLabelText(/Test Name/i), { target: { value: 'API Test' } });
    fireEvent.change(screen.getByLabelText(/Target URL/i), { target: { value: 'https://api.test.com' } });
    fireEvent.click(screen.getByText('Next'));

    // Step 2
    await waitFor(() => expect(screen.getByText('Load Test')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Load Test'));
    fireEvent.click(screen.getByText('Next'));

    // Step 3
    await waitFor(() => expect(screen.getByText('Virtual Users')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Next'));

    // Step 4 — submit
    await waitFor(() => expect(screen.getByText('Start Test')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Start Test'));

    await waitFor(() => {
      expect(testApi.createTest).toHaveBeenCalledTimes(1);
    });
    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/tests/42/live');
    });
  });

  it('shows error toast on API failure', async () => {
    vi.mocked(testApi.createTest).mockRejectedValue({ message: 'Server error' });
    renderWithRouter(<TestBuilderPage />);

    fireEvent.change(screen.getByLabelText(/Test Name/i), { target: { value: 'Test' } });
    fireEvent.change(screen.getByLabelText(/Target URL/i), { target: { value: 'https://api.test.com' } });
    fireEvent.click(screen.getByText('Next'));

    await waitFor(() => expect(screen.getByText('Load Test')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Load Test'));
    fireEvent.click(screen.getByText('Next'));

    await waitFor(() => expect(screen.getByText('Virtual Users')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Next'));

    await waitFor(() => expect(screen.getByText('Start Test')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Start Test'));

    await waitFor(() => {
      expect(screen.getByText('Server error')).toBeInTheDocument();
    });
  });

  it('pre-selects type from query param', () => {
    renderWithRouter(<TestBuilderPage />, '/tests/new?type=STRESS');
    // Should start on step 2 (type already pre-selected)
    expect(screen.getByText('Load Test')).toBeInTheDocument();
  });
});
