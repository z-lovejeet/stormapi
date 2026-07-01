import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ChevronUp, ChevronDown, Search, ArrowRight } from 'lucide-react';
import { useHistory } from '../hooks/useHistory';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { StatusBadge } from '../components/common/StatusBadge';
import { TestTypeBadge } from '../components/common/TestTypeBadge';
import { ComparisonView } from '../components/results/ComparisonView';
import { formatMs, formatPercent, formatDateTime } from '../utils/formatters';
import styles from './HistoryPage.module.css';

export function HistoryPage() {
  const navigate = useNavigate();
  const {
    tests,
    loading,
    page,
    totalPages,
    totalElements,
    filters,
    setFilters,
    sortField,
    sortDir,
    setSort,
    setPage,
    compareSelection,
    toggleCompare,
    clearCompare,
  } = useHistory();

  const [showComparison, setShowComparison] = useState(false);

  const SortIcon = ({ field }: { field: string }) => {
    const isActive = sortField === field;
    const Icon = isActive && sortDir === 'asc' ? ChevronUp : ChevronDown;
    return (
      <Icon
        size={12}
        className={`${styles.sortIcon} ${isActive ? styles.sortActive : ''}`}
      />
    );
  };

  if (loading && tests.length === 0) {
    return (
      <div className={styles.loadingWrapper}>
        <LoadingSpinner />
        <span>Loading test history…</span>
      </div>
    );
  }

  return (
    <div className={styles.page}>
      {/* ── Header ──────────────────────────────── */}
      <div className={styles.header}>
        <h1>Test History</h1>
        <span style={{ color: 'var(--storm-text-tertiary)', fontSize: 'var(--storm-text-sm)' }}>
          {totalElements} tests
        </span>
      </div>

      {/* ── Filters ─────────────────────────────── */}
      <div className={styles.filters}>
        <div style={{ position: 'relative' }}>
          <Search size={14} style={{ position: 'absolute', left: 10, top: '50%', transform: 'translateY(-50%)', color: 'var(--storm-text-tertiary)' }} />
          <input
            className={styles.searchInput}
            placeholder="Search tests…"
            style={{ paddingLeft: '2rem' }}
            value={filters.search || ''}
            onChange={(e) => setFilters({ search: e.target.value })}
          />
        </div>
        <select
          className={styles.filterSelect}
          value={filters.status || ''}
          onChange={(e) => setFilters({ status: e.target.value || undefined })}
        >
          <option value="">All Status</option>
          <option value="COMPLETED">Completed</option>
          <option value="RUNNING">Running</option>
          <option value="FAILED">Failed</option>
          <option value="CANCELLED">Cancelled</option>
        </select>
        <select
          className={styles.filterSelect}
          value={filters.type || ''}
          onChange={(e) => setFilters({ type: e.target.value || undefined })}
        >
          <option value="">All Types</option>
          <option value="LOAD">Load</option>
          <option value="STRESS">Stress</option>
          <option value="ENDURANCE">Endurance</option>
          <option value="SPIKE">Spike</option>
          <option value="BREAKPOINT">Breakpoint</option>
        </select>
      </div>

      {/* ── Compare Bar ─────────────────────────── */}
      {compareSelection.length > 0 && (
        <div className={styles.compareBar}>
          <span>{compareSelection.length}/2 selected for comparison</span>
          <div className={styles.compareActions}>
            {compareSelection.length === 2 && (
              <button
                className={`${styles.compareBtn} ${styles.compareBtnPrimary}`}
                onClick={() => setShowComparison(true)}
              >
                Compare <ArrowRight size={12} />
              </button>
            )}
            <button
              className={`${styles.compareBtn} ${styles.compareBtnSecondary}`}
              onClick={clearCompare}
            >
              Clear
            </button>
          </div>
        </div>
      )}

      {/* ── Table ───────────────────────────────── */}
      <table className={styles.table}>
        <thead>
          <tr>
            <th style={{ width: 40 }}></th>
            <th onClick={() => setSort('name')}>Name <SortIcon field="name" /></th>
            <th onClick={() => setSort('testType')}>Type <SortIcon field="testType" /></th>
            <th onClick={() => setSort('status')}>Status <SortIcon field="status" /></th>
            <th onClick={() => setSort('virtualUsers')}>Users <SortIcon field="virtualUsers" /></th>
            <th onClick={() => setSort('lastAvgResponseTimeMs')}>Avg Latency <SortIcon field="lastAvgResponseTimeMs" /></th>
            <th onClick={() => setSort('lastErrorRate')}>Error Rate <SortIcon field="lastErrorRate" /></th>
            <th onClick={() => setSort('totalRuns')}>Runs <SortIcon field="totalRuns" /></th>
            <th onClick={() => setSort('createdAt')}>Created <SortIcon field="createdAt" /></th>
          </tr>
        </thead>
        <tbody>
          {tests.map((t) => (
            <tr
              key={t.id}
              className={styles.clickableRow}
              onClick={() => navigate(`/tests/${t.id}/result`)}
            >
              <td onClick={(e) => e.stopPropagation()}>
                <input
                  type="checkbox"
                  className={styles.compareCheckbox}
                  checked={compareSelection.includes(t.id)}
                  onChange={() => toggleCompare(t.id)}
                  disabled={compareSelection.length >= 2 && !compareSelection.includes(t.id)}
                />
              </td>
              <td style={{ fontWeight: 500 }}>{t.name}</td>
              <td><TestTypeBadge type={t.testType} /></td>
              <td><StatusBadge status={t.status} /></td>
              <td>{t.virtualUsers}</td>
              <td>{t.lastAvgResponseTimeMs != null ? formatMs(t.lastAvgResponseTimeMs) : '—'}</td>
              <td>{t.lastErrorRate != null ? formatPercent(t.lastErrorRate) : '—'}</td>
              <td>{t.totalRuns}</td>
              <td>{formatDateTime(t.createdAt)}</td>
            </tr>
          ))}
          {tests.length === 0 && (
            <tr>
              <td colSpan={9} style={{ textAlign: 'center', padding: '3rem', color: 'var(--storm-text-tertiary)' }}>
                No tests found matching your filters.
              </td>
            </tr>
          )}
        </tbody>
      </table>

      {/* ── Pagination ──────────────────────────── */}
      {totalPages > 1 && (
        <div className={styles.pagination}>
          <span>Page {page + 1} of {totalPages}</span>
          <div className={styles.paginationBtns}>
            <button
              className={styles.pageBtn}
              disabled={page === 0}
              onClick={() => setPage(page - 1)}
            >
              Previous
            </button>
            <button
              className={styles.pageBtn}
              disabled={page >= totalPages - 1}
              onClick={() => setPage(page + 1)}
            >
              Next
            </button>
          </div>
        </div>
      )}

      {/* ── Comparison Modal ────────────────────── */}
      {showComparison && compareSelection.length === 2 && (
        <div
          className={styles.comparisonOverlay}
          onClick={() => setShowComparison(false)}
        >
          <div onClick={(e) => e.stopPropagation()}>
            <ComparisonView
              resultIdA={compareSelection[0]!}
              resultIdB={compareSelection[1]!}
              onClose={() => setShowComparison(false)}
            />
          </div>
        </div>
      )}
    </div>
  );
}
