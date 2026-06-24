import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { DataTable, type ColumnDef } from '../../components/common/DataTable';

interface TestRow {
  id: number;
  name: string;
  value: number;
}

const columns: ColumnDef<TestRow>[] = [
  { key: 'id', header: 'ID', sortable: true },
  { key: 'name', header: 'Name', sortable: true },
  { key: 'value', header: 'Value', render: (row) => `$${row.value}` },
];

const data: TestRow[] = [
  { id: 1, name: 'Alpha', value: 100 },
  { id: 2, name: 'Beta', value: 200 },
  { id: 3, name: 'Gamma', value: 50 },
];

describe('DataTable', () => {
  it('renders all headers', () => {
    render(<DataTable columns={columns} data={data} keyExtractor={(r) => r.id} />);
    expect(screen.getByText('ID')).toBeInTheDocument();
    expect(screen.getByText('Name')).toBeInTheDocument();
    expect(screen.getByText('Value')).toBeInTheDocument();
  });

  it('renders all rows', () => {
    render(<DataTable columns={columns} data={data} keyExtractor={(r) => r.id} />);
    expect(screen.getByText('Alpha')).toBeInTheDocument();
    expect(screen.getByText('Beta')).toBeInTheDocument();
    expect(screen.getByText('Gamma')).toBeInTheDocument();
  });

  it('uses custom render', () => {
    render(<DataTable columns={columns} data={data} keyExtractor={(r) => r.id} />);
    expect(screen.getByText('$100')).toBeInTheDocument();
  });

  it('shows empty state when no data', () => {
    render(<DataTable columns={columns} data={[]} keyExtractor={(r) => r.id} emptyMessage="No tests found" />);
    expect(screen.getByText('No tests found')).toBeInTheDocument();
  });

  it('calls onRowClick', () => {
    const handler = vi.fn();
    render(<DataTable columns={columns} data={data} keyExtractor={(r) => r.id} onRowClick={handler} />);
    fireEvent.click(screen.getByText('Alpha'));
    expect(handler).toHaveBeenCalledWith(data[0]);
  });

  it('shows loading state', () => {
    render(<DataTable columns={columns} data={[]} loading keyExtractor={(r) => r.id} />);
    expect(screen.getByRole('status')).toBeInTheDocument();
  });
});
