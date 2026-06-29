import { useState, useEffect } from 'react';
import type { ComparisonResponse } from '../types/result';
import { compareResults } from '../api/resultApi';

export interface UseComparisonReturn {
  comparison: ComparisonResponse | null;
  loading: boolean;
  error: string | null;
}

export function useComparison(
  resultIdA: number | null,
  resultIdB: number | null,
): UseComparisonReturn {
  const [comparison, setComparison] = useState<ComparisonResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (resultIdA == null || resultIdB == null) {
      setComparison(null);
      return;
    }

    let cancelled = false;
    setLoading(true);
    setError(null);

    compareResults(resultIdA, resultIdB)
      .then((data) => {
        if (!cancelled) setComparison(data);
      })
      .catch((err) => {
        if (!cancelled) setError(err instanceof Error ? err.message : 'Comparison failed');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => { cancelled = true; };
  }, [resultIdA, resultIdB]);

  return { comparison, loading, error };
}
