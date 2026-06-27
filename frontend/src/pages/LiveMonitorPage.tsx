import { useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  Activity,
  Zap,
  Clock,
  AlertTriangle,
  ArrowLeft,
  Square,
} from 'lucide-react';
import { useLiveMonitor } from '../hooks/useLiveMonitor';
import { stopTest } from '../api/testApi';
import { LiveLineChart } from '../components/charts/LiveLineChart';
import { LiveKpiCard } from '../components/live-monitor/LiveKpiCard';
import { ProgressBar } from '../components/live-monitor/ProgressBar';
import { RequestLogTable } from '../components/live-monitor/RequestLogTable';
import { CompletionBanner } from '../components/live-monitor/CompletionBanner';
import { ConnectionStatus } from '../components/live-monitor/ConnectionStatus';
import { Button } from '../components/common/Button';
import { Modal } from '../components/common/Modal';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { StatusBadge } from '../components/common/StatusBadge';
import { TestTypeBadge } from '../components/common/TestTypeBadge';
import { useToast } from '../components/common/Toast';
import {
  formatMs,
  formatRps,
  formatPercent,
  formatNumber,
} from '../utils/formatters';
import styles from './LiveMonitorPage.module.css';

export function LiveMonitorPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { showToast } = useToast();
  const testId = Number(id);

  const {
    testConfig,
    configLoading,
    configError,
    connectionState,
    kpis,
    chartData,
    logEntries,
    elapsedSeconds,
    completionState,
  } = useLiveMonitor(testId);

  const [stopping, setStopping] = useState(false);
  const [showStopModal, setShowStopModal] = useState(false);
  const [logPaused, setLogPaused] = useState(false);

  const handleStopConfirm = useCallback(async () => {
    setStopping(true);
    try {
      await stopTest(testId);
      showToast('success', 'Test stopped');
      setShowStopModal(false);
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to stop test';
      // 409 = already completed — treat as success
      if (message.includes('409') || message.includes('Conflict')) {
        showToast('info', 'Test already completed');
        setShowStopModal(false);
      } else {
        showToast('error', message);
      }
    } finally {
      setStopping(false);
    }
  }, [testId, showToast]);

  const toggleLogPause = useCallback(() => {
    setLogPaused((p) => !p);
  }, []);

  const hasMetrics = chartData.responseTime.length > 0;

  // Loading state
  if (configLoading) {
    return (
      <div className={styles.centerState}>
        <LoadingSpinner />
        <span>Loading test configuration…</span>
      </div>
    );
  }

  // Error state
  if (configError || !testConfig) {
    return (
      <div className={styles.centerState}>
        <AlertTriangle size={48} color="var(--storm-error)" />
        <span className={styles.errorText}>{configError || 'Test not found'}</span>
        <Button variant="secondary" icon={ArrowLeft} onClick={() => navigate('/dashboard')}>
          Back to Dashboard
        </Button>
      </div>
    );
  }

  const progressStatus = completionState.completed
    ? completionState.status === 'completed' ? 'completed' : 'stopped'
    : 'running';

  return (
    <motion.div
      className={styles.page}
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ duration: 0.2 }}
    >
      {/* Header */}
      <div className={styles.header}>
        <div className={styles.headerLeft}>
          <h1 className={styles.testName}>{testConfig.name}</h1>
          <div className={styles.headerBadges}>
            <TestTypeBadge type={testConfig.testType} />
            <StatusBadge status={completionState.completed
              ? (completionState.status === 'completed' ? 'COMPLETED' : completionState.status === 'failed' ? 'FAILED' : 'CANCELLED') as import('../types/test').TestStatus
              : testConfig.status
            } />
          </div>
        </div>
        <div className={styles.headerRight}>
          <ConnectionStatus state={connectionState} />
        </div>
      </div>

      {/* Completion Banner */}
      <CompletionBanner
        testId={testId}
        status={completionState.status}
        visible={completionState.completed}
      />

      {/* Progress Bar */}
      <ProgressBar
        elapsedSeconds={elapsedSeconds}
        totalSeconds={testConfig.durationSeconds}
        status={progressStatus}
      />

      {/* KPI Cards */}
      <div className={styles.kpiGrid} aria-live="polite">
        <LiveKpiCard
          icon={Activity}
          label="Total Requests"
          value={kpis.totalRequests}
          formatter={(n) => formatNumber(Math.round(n))}
          loading={!hasMetrics && !completionState.completed}
        />
        <LiveKpiCard
          icon={Zap}
          label="Throughput"
          value={kpis.throughputRps}
          formatter={(n) => formatRps(n)}
          loading={!hasMetrics && !completionState.completed}
        />
        <LiveKpiCard
          icon={Clock}
          label="Avg Response Time"
          value={kpis.avgResponseTimeMs}
          formatter={(n) => formatMs(n)}
          loading={!hasMetrics && !completionState.completed}
        />
        <LiveKpiCard
          icon={AlertTriangle}
          label="Error Rate"
          value={kpis.errorRate}
          formatter={(n) => formatPercent(n)}
          loading={!hasMetrics && !completionState.completed}
        />
      </div>

      {/* Charts Grid */}
      <div className={styles.chartsGrid}>
        <LiveLineChart
          data={chartData.responseTime}
          label="Response Time"
          color="#3b82f6"
          unit="ms"
          formatValue={(v) => formatMs(v)}
          currentValue={hasMetrics ? formatMs(kpis.avgResponseTimeMs) : undefined}
          animate={chartData.responseTime.length <= 1}
        />
        <LiveLineChart
          data={chartData.throughput}
          label="Throughput"
          color="#10b981"
          unit="rps"
          formatValue={(v) => formatRps(v)}
          currentValue={hasMetrics ? formatRps(kpis.throughputRps) : undefined}
          animate={chartData.throughput.length <= 1}
        />
        <LiveLineChart
          data={chartData.errorRate}
          label="Error Rate"
          color="#ef4444"
          unit="%"
          formatValue={(v) => formatPercent(v)}
          currentValue={hasMetrics ? formatPercent(kpis.errorRate) : undefined}
          animate={chartData.errorRate.length <= 1}
        />
        <LiveLineChart
          data={chartData.activeUsers}
          label="Active Users"
          color="#8b5cf6"
          formatValue={(v) => String(Math.round(v))}
          currentValue={hasMetrics ? String(Math.round(kpis.totalRequests > 0 ? chartData.activeUsers[chartData.activeUsers.length - 1]?.value ?? 0 : 0)) : undefined}
          animate={chartData.activeUsers.length <= 1}
        />
      </div>

      {/* Request Log */}
      <RequestLogTable
        entries={logEntries}
        paused={logPaused}
        onTogglePause={toggleLogPause}
      />

      {/* Controls */}
      <div className={styles.controls}>
        <div className={styles.controlsLeft}>
          <Button
            variant="secondary"
            icon={ArrowLeft}
            onClick={() => navigate('/dashboard')}
          >
            Dashboard
          </Button>
        </div>
        {!completionState.completed && (
          <Button
            variant="danger"
            icon={Square}
            onClick={() => setShowStopModal(true)}
            loading={stopping}
          >
            Stop Test
          </Button>
        )}
      </div>

      {/* Stop Confirmation Modal */}
      <Modal
        isOpen={showStopModal}
        onClose={() => setShowStopModal(false)}
        title="Stop Test?"
        footer={
          <>
            <Button variant="secondary" onClick={() => setShowStopModal(false)}>
              Cancel
            </Button>
            <Button variant="danger" onClick={handleStopConfirm} loading={stopping}>
              Stop Test
            </Button>
          </>
        }
      >
        <p>Are you sure you want to stop this test? Results collected so far will be saved.</p>
      </Modal>
    </motion.div>
  );
}
