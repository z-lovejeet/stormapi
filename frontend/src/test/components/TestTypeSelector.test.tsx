import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { TestTypeSelector } from '../../components/test-builder/TestTypeSelector';
import { TestType } from '../../types/test';

describe('TestTypeSelector', () => {
  it('renders all 6 test type cards', () => {
    render(<TestTypeSelector selectedType={undefined} onSelect={vi.fn()} />);
    expect(screen.getByText('Load Test')).toBeInTheDocument();
    expect(screen.getByText('Stress Test')).toBeInTheDocument();
    expect(screen.getByText('Spike Test')).toBeInTheDocument();
    expect(screen.getByText('Soak Test')).toBeInTheDocument();
    expect(screen.getByText('Breakpoint Test')).toBeInTheDocument();
    expect(screen.getByText('Scalability Test')).toBeInTheDocument();
  });

  it('calls onSelect when card is clicked', () => {
    const onSelect = vi.fn();
    render(<TestTypeSelector selectedType={undefined} onSelect={onSelect} />);
    fireEvent.click(screen.getByText('Stress Test'));
    expect(onSelect).toHaveBeenCalledWith(TestType.STRESS);
  });

  it('marks selected card with aria-checked', () => {
    render(<TestTypeSelector selectedType={TestType.SPIKE} onSelect={vi.fn()} />);
    const spikeCard = screen.getByRole('radio', { name: 'Spike Test' });
    expect(spikeCard).toHaveAttribute('aria-checked', 'true');
    const loadCard = screen.getByRole('radio', { name: 'Load Test' });
    expect(loadCard).toHaveAttribute('aria-checked', 'false');
  });

  it('selects via keyboard Enter', () => {
    const onSelect = vi.fn();
    render(<TestTypeSelector selectedType={undefined} onSelect={onSelect} />);
    const card = screen.getByRole('radio', { name: 'Soak Test' });
    fireEvent.keyDown(card, { key: 'Enter' });
    expect(onSelect).toHaveBeenCalledWith(TestType.SOAK);
  });
});
