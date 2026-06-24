import { Plus } from 'lucide-react';
import { EmptyState } from '../components/common/EmptyState';

export function TestBuilderPage() {
  return (
    <EmptyState
      icon={Plus}
      title="Test Builder"
      description="Multi-step wizard for configuring and launching performance tests. Coming in Phase 12."
    />
  );
}
