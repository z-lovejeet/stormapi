import { describe, it, expect } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import type { ReactNode } from 'react';
import { ThemeProvider } from '../../context/ThemeContext';
import { useTheme } from '../../hooks/useTheme';

function wrapper({ children }: { children: ReactNode }) {
  return <ThemeProvider>{children}</ThemeProvider>;
}

describe('useTheme', () => {
  it('returns theme and toggle function', () => {
    const { result } = renderHook(() => useTheme(), { wrapper });
    expect(result.current.theme).toBeDefined();
    expect(typeof result.current.toggleTheme).toBe('function');
    expect(typeof result.current.setTheme).toBe('function');
  });

  it('toggles theme', () => {
    const { result } = renderHook(() => useTheme(), { wrapper });
    const initial = result.current.theme;
    act(() => result.current.toggleTheme());
    expect(result.current.theme).not.toBe(initial);
  });

  it('sets theme explicitly', () => {
    const { result } = renderHook(() => useTheme(), { wrapper });
    act(() => result.current.setTheme('dark'));
    expect(result.current.theme).toBe('dark');
    act(() => result.current.setTheme('light'));
    expect(result.current.theme).toBe('light');
  });

  it('throws when used outside provider', () => {
    expect(() => {
      renderHook(() => useTheme());
    }).toThrow('useTheme must be used within a ThemeProvider');
  });
});
