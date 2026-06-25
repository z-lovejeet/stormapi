import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { StepIndicator } from '../../components/test-builder/StepIndicator';

const steps = [
  { label: 'Target' },
  { label: 'Test Type' },
  { label: 'Configuration' },
  { label: 'Review & Run' },
] as const;

describe('StepIndicator', () => {
  it('renders all 4 step labels', () => {
    render(<StepIndicator steps={steps} currentStep={0} />);
    expect(screen.getByText('Target')).toBeInTheDocument();
    expect(screen.getByText('Test Type')).toBeInTheDocument();
    expect(screen.getByText('Configuration')).toBeInTheDocument();
    expect(screen.getByText('Review & Run')).toBeInTheDocument();
  });

  it('marks the active step with aria-current', () => {
    render(<StepIndicator steps={steps} currentStep={1} />);
    const active = screen.getByLabelText('Step 2: Test Type');
    expect(active).toHaveAttribute('aria-current', 'step');
  });

  it('marks completed steps with completed label', () => {
    render(<StepIndicator steps={steps} currentStep={2} />);
    const step1 = screen.getByLabelText('Step 1: Target (completed)');
    expect(step1).toBeInTheDocument();
  });

  it('calls onStepClick for completed steps only', () => {
    const onClick = vi.fn();
    render(<StepIndicator steps={steps} currentStep={2} onStepClick={onClick} />);
    // Completed step should be clickable
    fireEvent.click(screen.getByLabelText('Step 1: Target (completed)'));
    expect(onClick).toHaveBeenCalledWith(0);
    // Future step should NOT be clickable
    fireEvent.click(screen.getByLabelText('Step 4: Review & Run'));
    expect(onClick).toHaveBeenCalledTimes(1);
  });
});
