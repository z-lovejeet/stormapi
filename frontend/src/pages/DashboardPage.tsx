import { LayoutDashboard } from 'lucide-react';
import { EmptyState } from '../components/common/EmptyState';

export function DashboardPage() {
  return (
    <EmptyState
      icon={LayoutDashboard}
      title="Dashboard"
      description="Aggregated test statistics, KPI cards, and recent test history. Coming in Phase 11."
    />
  );
}
