import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { Search } from 'lucide-react';
import { Input } from '../../components/common/Input';

describe('Input', () => {
  it('renders label', () => {
    render(<Input label="URL" />);
    expect(screen.getByLabelText('URL')).toBeInTheDocument();
  });

  it('renders placeholder', () => {
    render(<Input placeholder="Enter URL" />);
    expect(screen.getByPlaceholderText('Enter URL')).toBeInTheDocument();
  });

  it('renders error message', () => {
    render(<Input label="URL" error="URL is required" />);
    expect(screen.getByRole('alert')).toHaveTextContent('URL is required');
  });

  it('renders helper text', () => {
    render(<Input helperText="Enter a valid URL" />);
    expect(screen.getByText('Enter a valid URL')).toBeInTheDocument();
  });

  it('renders icon', () => {
    const { container } = render(<Input icon={Search} />);
    expect(container.querySelector('.icon')).toBeInTheDocument();
  });

  it('calls onChange', () => {
    const handler = vi.fn();
    render(<Input onChange={handler} />);
    fireEvent.change(screen.getByRole('textbox'), { target: { value: 'test' } });
    expect(handler).toHaveBeenCalled();
  });

  it('sets aria-invalid on error', () => {
    render(<Input label="Email" error="Invalid" />);
    expect(screen.getByLabelText('Email')).toHaveAttribute('aria-invalid', 'true');
  });
});
