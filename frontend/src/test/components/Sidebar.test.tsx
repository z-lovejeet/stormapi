import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import type { ReactNode } from 'react';
import { ThemeProvider } from '../../context/ThemeContext';
import { Sidebar } from '../../components/layout/Sidebar';

function wrapper({ children }: { children: ReactNode }) {
  return (
    <MemoryRouter>
      <ThemeProvider>{children}</ThemeProvider>
    </MemoryRouter>
  );
}

describe('Sidebar', () => {
  it('renders logo text', () => {
    render(<Sidebar />, { wrapper });
    expect(screen.getByText('StormAPI')).toBeInTheDocument();
  });

  it('renders all nav items', () => {
    render(<Sidebar />, { wrapper });
    expect(screen.getByText('Dashboard')).toBeInTheDocument();
    expect(screen.getByText('New Test')).toBeInTheDocument();
    expect(screen.getByText('History')).toBeInTheDocument();
    expect(screen.getByText('Collections')).toBeInTheDocument();
    expect(screen.getByText('Settings')).toBeInTheDocument();
  });

  it('renders nav with correct role', () => {
    render(<Sidebar />, { wrapper });
    expect(screen.getByRole('navigation', { name: 'Main navigation' })).toBeInTheDocument();
  });

  it('renders version info', () => {
    render(<Sidebar />, { wrapper });
    expect(screen.getByText(/StormAPI v1/)).toBeInTheDocument();
  });
});
