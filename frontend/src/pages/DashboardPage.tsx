import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  FlaskConical,
  Send,
  Timer,
  Gauge,
  Zap,
  Flame,
  Rocket,
  Eye,
  RefreshCw,
  Trash2,
  RotateCcw,
} from 'lucide-react';
import { useDashboard } from '../hooks/useDashboard';
import { KpiCard } from '../components/common/KpiCard';
import { DataTable, type ColumnDef } from '../components/common/DataTable';
import { StatusBadge } from '../components/common/StatusBadge';
import { TestTypeBadge } from '../components/common/TestTypeBadge';
import { EmptyState } from '../components/common/EmptyState';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { Button } from '../components/common/Button';
import { useToast } from '../components/common/Toast';
import { formatMs, formatRps, formatNumber, formatDateTime, formatDuration } from '../utils/formatters';
import { deleteTest, rerunTest } from '../api/testApi';
import type { TestSummaryResponse } from '../types/test';
import styles from './DashboardPage.module.css';

const fadeUp = {
  initial: { opacity: 0, y: 8 },
  animate: { opacity: 1, y: 0 },
};

export function DashboardPage() {
  const navigate = useNavigate();
  const { stats, loading, error, refresh } = useDashboard();
  const { showToast } = useToast();

  const handleRerun = async (e: React.MouseEvent, id: number, name: string) => {
    e.stopPropagation();
    try {
      await rerunTest(id);
      showToast('success', `Re-running "${name}"`);
      refresh();
    } catch {
      showToast('error', `Failed to re-run "${name}"`);
    }
  };

  const handleDelete = async (e: React.MouseEvent, id: number, name: string) => {
    e.stopPropagation();
    if (!window.confirm(`Delete test "${name}"? This cannot be undone.`)) return;
    try {
      await deleteTest(id);
      showToast('success', `Deleted "${name}"`);
      refresh();
    } catch {
      showToast('error', `Failed to delete "${name}"`);
    }
  };

  const columns: ColumnDef<TestSummaryResponse>[] = [
    { key: 'name', header: 'Name' },
    {
      key: 'testType',
      header: 'Type',
      render: (row) => <TestTypeBadge type={row.testType} />,
    },
    {
      key: 'targetUrl',
      header: 'URL',
      render: (row) => (
        <span className={styles.urlCell} title={row.targetUrl}>
          {row.targetUrl}
        </span>
      ),
    },
    {
      key: 'status',
      header: 'Status',
      render: (row) => <StatusBadge status={row.status} />,
    },
    {
      key: 'durationSeconds',
      header: 'Duration',
      render: (row) => formatDuration(row.durationSeconds),
    },
    {
      key: 'createdAt',
      header: 'Date',
      render: (row) => formatDateTime(row.createdAt),
    },
    {
      key: 'actions',
      header: 'Actions',
      render: (row) => (
        <div className={styles.actionsCell}>
          <Button
            variant="ghost"
            size="sm"
            icon={Eye}
            iconOnly
            aria-label={`View test ${row.name}`}
            onClick={(e) => {
              e.stopPropagation();
              navigate(`/tests/${row.id}/result`);
            }}
          />
          <Button
            variant="ghost"
            size="sm"
            icon={RefreshCw}
            iconOnly
            aria-label={`Re-run test ${row.name}`}
            onClick={(e) => handleRerun(e, row.id, row.name)}
          />
          <Button
            variant="ghost"
            size="sm"
            icon={Trash2}
            iconOnly
            aria-label={`Delete test ${row.name}`}
            onClick={(e) => handleDelete(e, row.id, row.name)}
          />
        </div>
      ),
    },
  ];

  // ── Loading State ──
  if (loading && !stats) {
    return (
      <div className={styles.loadingWrapper} role="status" aria-live="polite">
        <LoadingSpinner />
      </div>
    );
  }

  const isEmpty = stats !== null && stats.totalTests === 0;

  return (
    <div className={styles.page}>
      {/* Header */}
      <div className={styles.header}>
        <h1>Dashboard</h1>
        <Button variant="ghost" size="sm" icon={RotateCcw} onClick={refresh} loading={loading}>
          Refresh
        </Button>
      </div>

      {/* Error Banner */}
      {error && (
        <div className={styles.errorBanner} role="alert">
          <p>⚠ {error}</p>
          <Button variant="secondary" size="sm" onClick={refresh}>
            Retry
          </Button>
        </div>
      )}

      {/* KPI Cards */}
      <div className={styles.kpiGrid}>
        <KpiCard
          icon={FlaskConical}
          label="Total Tests"
          value={stats ? formatNumber(stats.totalTests) : '—'}
          loading={loading}
        />
        <KpiCard
          icon={Send}
          label="Total Requests"
          value={stats ? formatNumber(stats.totalRequestsSent) : '—'}
          loading={loading}
        />
        <KpiCard
          icon={Timer}
          label="Avg Response Time"
          value={stats ? formatMs(stats.avgResponseTimeMs) : '—'}
          loading={loading}
        />
        <KpiCard
          icon={Gauge}
          label="Avg Throughput"
          value={stats ? formatRps(stats.avgThroughputRps) : '—'}
          loading={loading}
        />
      </div>

      {/* Quick Actions */}
      <motion.div
        className={styles.quickActions}
        {...fadeUp}
        transition={{ delay: 0.32, duration: 0.2 }}
      >
        <Button icon={Zap} onClick={() => navigate('/tests/new?type=LOAD')}>
          New Load Test
        </Button>
        <Button variant="secondary" icon={Flame} onClick={() => navigate('/tests/new?type=STRESS')}>
          New Stress Test
        </Button>
      </motion.div>

      {/* Recent Tests */}
      {isEmpty ? (
        <motion.div {...fadeUp} transition={{ delay: 0.4, duration: 0.3 }}>
          <EmptyState
            icon={Rocket}
            title="No Tests Yet"
            description="Create your first performance test to see results here."
          >
            <Button
              icon={Zap}
              onClick={() => navigate('/tests/new')}
              style={{ marginTop: 'var(--storm-space-4)' }}
            >
              Create Your First Test
            </Button>
          </EmptyState>
        </motion.div>
      ) : (
        stats && (
          <motion.div className={styles.section} {...fadeUp} transition={{ delay: 0.4, duration: 0.3 }}>
            <div className={styles.sectionHeader}>
              <h2>Recent Tests</h2>
            </div>
            <DataTable<TestSummaryResponse>
              columns={columns}
              data={stats.recentTests}
              keyExtractor={(row) => row.id}
              onRowClick={(row) => navigate(`/tests/${row.id}/result`)}
              emptyMessage="No recent tests"
            />
          </motion.div>
        )
      )}
    </div>
  );
}
