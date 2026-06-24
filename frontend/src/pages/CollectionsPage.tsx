import { FolderOpen } from 'lucide-react';
import { EmptyState } from '../components/common/EmptyState';

export function CollectionsPage() {
  return (
    <EmptyState
      icon={FolderOpen}
      title="API Collections"
      description="Organize and manage frequently tested API endpoints. Coming in Phase 15."
    />
  );
}
