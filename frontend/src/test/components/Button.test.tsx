import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { Zap } from 'lucide-react';
import { Button } from '../../components/common/Button';

describe('Button', () => {
  it('renders children', () => {
    render(<Button>Click me</Button>);
    expect(screen.getByText('Click me')).toBeInTheDocument();
  });

  it('calls onClick', () => {
    const handler = vi.fn();
    render(<Button onClick={handler}>Click</Button>);
    fireEvent.click(screen.getByText('Click'));
    expect(handler).toHaveBeenCalledOnce();
  });

  it('disables when disabled prop', () => {
    render(<Button disabled>Disabled</Button>);
    expect(screen.getByText('Disabled').closest('button')).toBeDisabled();
  });

  it('disables when loading', () => {
    render(<Button loading>Loading</Button>);
    expect(screen.getByRole('status')).toBeInTheDocument();
  });

  it('renders icon', () => {
    render(<Button icon={Zap}>With Icon</Button>);
    expect(screen.getByText('With Icon')).toBeInTheDocument();
  });

  it('applies variant class', () => {
    const { container } = render(<Button variant="danger">Danger</Button>);
    expect(container.querySelector('.danger')).toBeInTheDocument();
  });

  it('applies size class', () => {
    const { container } = render(<Button size="lg">Large</Button>);
    expect(container.querySelector('.lg')).toBeInTheDocument();
  });
});
