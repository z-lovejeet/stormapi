import { useState, useEffect, useCallback, useMemo } from 'react';
import type { TestSummaryResponse, TestStatus, TestType } from '../types/test';
import type { HistoryFilters } from '../types/result';
import { listTests } from '../api/testApi';

export interface UseHistoryReturn {
  tests: TestSummaryResponse[];
  loading: boolean;
  error: string | null;
  page: number;
  totalPages: number;
  totalElements: number;
  filters: HistoryFilters;
  setFilters: (f: Partial<HistoryFilters>) => void;
  sortField: string;
  sortDir: 'asc' | 'desc';
  setSort: (field: string) => void;
  setPage: (p: number) => void;
  compareSelection: number[];
  toggleCompare: (id: number) => void;
  clearCompare: () => void;
  refresh: () => void;
}

export function useHistory(): UseHistoryReturn {
  const [tests, setTests] = useState<TestSummaryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [filters, setFiltersState] = useState<HistoryFilters>({});
  const [sortField, setSortField] = useState('createdAt');
  const [sortDir, setSortDir] = useState<'asc' | 'desc'>('desc');
  const [compareSelection, setCompareSelection] = useState<number[]>([]);

  const setFilters = useCallback((partial: Partial<HistoryFilters>) => {
    setFiltersState((prev) => ({ ...prev, ...partial }));
    setPage(0);
  }, []);

  const setSort = useCallback((field: string) => {
    setSortDir((prev) => (sortField === field ? (prev === 'asc' ? 'desc' : 'asc') : 'desc'));
    setSortField(field);
    setPage(0);
  }, [sortField]);

  const toggleCompare = useCallback((id: number) => {
    setCompareSelection((prev) => {
      if (prev.includes(id)) return prev.filter((x) => x !== id);
      if (prev.length >= 2) return prev;
      return [...prev, id];
    });
  }, []);

  const clearCompare = useCallback(() => setCompareSelection([]), []);

  const sortParam = useMemo(() => `${sortField},${sortDir}`, [sortField, sortDir]);

  const fetchData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await listTests({
        page,
        size: 20,
        sort: sortParam,
        status: (filters.status as TestStatus) || undefined,
        type: (filters.type as TestType) || undefined,
      });
      setTests(res.content);
      setTotalPages(res.totalPages);
      setTotalElements(res.totalElements);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load tests');
    } finally {
      setLoading(false);
    }
  }, [page, sortParam, filters.status, filters.type]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  return {
    tests, loading, error, page, totalPages, totalElements,
    filters, setFilters, sortField, sortDir, setSort, setPage,
    compareSelection, toggleCompare, clearCompare, refresh: fetchData,
  };
}
