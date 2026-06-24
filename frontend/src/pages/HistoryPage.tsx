import { History } from 'lucide-react';
import { EmptyState } from '../components/common/EmptyState';

export function HistoryPage() {
  return (
    <EmptyState
      icon={History}
      title="Test History"
      description="All past tests with filtering, sorting, and side-by-side comparison. Coming in Phase 14."
    />
  );
}
