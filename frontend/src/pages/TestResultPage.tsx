import { BarChart3 } from 'lucide-react';
import { EmptyState } from '../components/common/EmptyState';

export function TestResultPage() {
  return (
    <EmptyState
      icon={BarChart3}
      title="Test Results"
      description="Detailed post-test report with charts, percentile distribution, and request logs. Coming in Phase 14."
    />
  );
}
