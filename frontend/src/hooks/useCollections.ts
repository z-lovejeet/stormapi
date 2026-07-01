import { useState, useEffect, useCallback } from 'react';
import type { ApiCollection } from '../types/collection';
import {
  listCollections,
  createCollection,
  updateCollection,
  deleteCollection,
} from '../api/collectionApi';

interface UseCollectionsReturn {
  collections: ApiCollection[];
  loading: boolean;
  error: string | null;
  refresh: () => void;
  create: (name: string, description?: string) => Promise<void>;
  update: (id: number, name: string, description?: string) => Promise<void>;
  remove: (id: number) => Promise<void>;
}

/**
 * Hook for managing the collections list with CRUD operations.
 */
export function useCollections(): UseCollectionsReturn {
  const [collections, setCollections] = useState<ApiCollection[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchCollections = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await listCollections();
      setCollections(data);
    } catch {
      setError('Failed to load collections');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchCollections();
  }, [fetchCollections]);

  const create = useCallback(
    async (name: string, description?: string) => {
      await createCollection({ name, description });
      await fetchCollections();
    },
    [fetchCollections],
  );

  const update = useCallback(
    async (id: number, name: string, description?: string) => {
      await updateCollection(id, { name, description });
      await fetchCollections();
    },
    [fetchCollections],
  );

  const remove = useCallback(
    async (id: number) => {
      // Optimistic delete
      setCollections((prev) => prev.filter((c) => c.id !== id));
      try {
        await deleteCollection(id);
      } catch {
        await fetchCollections(); // Revert on failure
      }
    },
    [fetchCollections],
  );

  return {
    collections,
    loading,
    error,
    refresh: fetchCollections,
    create,
    update,
    remove,
  };
}
