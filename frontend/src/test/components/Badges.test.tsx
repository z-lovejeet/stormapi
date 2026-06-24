import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { StatusBadge } from '../../components/common/StatusBadge';
import { MethodBadge } from '../../components/common/MethodBadge';
import { TestStatus, HttpMethod } from '../../types/test';

describe('StatusBadge', () => {
  it('renders RUNNING status', () => {
    render(<StatusBadge status={TestStatus.RUNNING} />);
    expect(screen.getByText('Running')).toBeInTheDocument();
  });

  it('renders COMPLETED status', () => {
    render(<StatusBadge status={TestStatus.COMPLETED} />);
    expect(screen.getByText('Completed')).toBeInTheDocument();
  });

  it('renders FAILED status', () => {
    render(<StatusBadge status={TestStatus.FAILED} />);
    expect(screen.getByText('Failed')).toBeInTheDocument();
  });

  it('renders all status variants', () => {
    const statuses = Object.values(TestStatus);
    statuses.forEach((status) => {
      const { unmount } = render(<StatusBadge status={status} />);
      unmount();
    });
  });
});

describe('MethodBadge', () => {
  it('renders GET', () => {
    render(<MethodBadge method={HttpMethod.GET} />);
    expect(screen.getByText('GET')).toBeInTheDocument();
  });

  it('renders POST', () => {
    render(<MethodBadge method={HttpMethod.POST} />);
    expect(screen.getByText('POST')).toBeInTheDocument();
  });

  it('renders DELETE', () => {
    render(<MethodBadge method={HttpMethod.DELETE} />);
    expect(screen.getByText('DELETE')).toBeInTheDocument();
  });
});
