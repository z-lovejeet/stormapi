import { Activity } from 'lucide-react';
import { EmptyState } from '../components/common/EmptyState';

export function LiveMonitorPage() {
  return (
    <EmptyState
      icon={Activity}
      title="Live Monitor"
      description="Real-time charts, streaming KPIs, and request logs powered by WebSocket. Coming in Phase 13."
    />
  );
}
