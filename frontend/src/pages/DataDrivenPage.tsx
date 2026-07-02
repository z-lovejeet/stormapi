import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Play, CheckCircle, XCircle, Loader } from 'lucide-react';
import { scenarioApi } from '../api/scenarioApi';
import { dataApi } from '../api/dataApi';
import DataUpload from '../components/data/DataUpload';
import DataPreviewTable from '../components/data/DataPreviewTable';
import AssertionBuilder from '../components/assertions/AssertionBuilder';
import type { TestScenario } from '../types/scenario';
import type {
  AssertionDefinition,
  DataFormat,
  DataDrivenExecutionResponse,
} from '../types/data';
import styles from './DataDrivenPage.module.css';

/**
 * Data-Driven Testing page.
 * Lets users select a scenario, upload/paste CSV or JSON data,
 * define assertions, and execute the scenario once per data row.
 */
const DataDrivenPage: React.FC = () => {
  // ── State ─────────────────────────────────────
  const [scenarios, setScenarios] = useState<TestScenario[]>([]);
  const [selectedScenarioId, setSelectedScenarioId] = useState<number | null>(null);
  const [format, setFormat] = useState<DataFormat>('CSV');
  const [dataContent, setDataContent] = useState('');
  const [assertions, setAssertions] = useState<AssertionDefinition[]>([]);
  const [result, setResult] = useState<DataDrivenExecutionResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // ── Load scenarios ────────────────────────────
  useEffect(() => {
    scenarioApi.getAll().then(setScenarios).catch(console.error);
  }, []);

  // ── Parse preview data ────────────────────────
  const previewRows = useMemo(() => {
    if (!dataContent.trim()) return [];
    try {
      if (format === 'JSON') {
        const parsed = JSON.parse(dataContent);
        if (Array.isArray(parsed)) {
          return parsed.map((item: Record<string, unknown>) => {
            const row: Record<string, string> = {};
            for (const [k, v] of Object.entries(item)) {
              row[k] = String(v ?? '');
            }
            return row;
          });
        }
      } else {
        // Simple CSV preview parse
        const lines = dataContent.split('\n').filter((l) => l.trim());
        if (lines.length < 2) return [];
        const headers = lines[0].split(',').map((h) => h.trim());
        return lines.slice(1).map((line) => {
          const values = line.split(',');
          const row: Record<string, string> = {};
          headers.forEach((h, i) => {
            row[h] = (values[i] || '').trim();
          });
          return row;
        });
      }
    } catch {
      // Preview parse failure is non-fatal
    }
    return [];
  }, [dataContent, format]);

  // ── Execute ───────────────────────────────────
  const handleExecute = useCallback(async () => {
    if (!selectedScenarioId || !dataContent.trim()) return;

    setLoading(true);
    setError(null);
    setResult(null);

    try {
      const response = await dataApi.execute({
        scenarioId: selectedScenarioId,
        format,
        dataContent,
        assertions: assertions.length > 0 ? assertions : undefined,
      });
      setResult(response);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message
        : typeof err === 'object' && err !== null && 'message' in err
          ? String((err as { message: string }).message)
          : 'Execution failed';
      setError(msg);
    } finally {
      setLoading(false);
    }
  }, [selectedScenarioId, format, dataContent, assertions]);

  return (
    <div className={styles.page}>
      <h1 className={styles.pageTitle}>Data-Driven Testing</h1>
      <p className={styles.pageSubtitle}>
        Execute scenarios with parameterized data. Upload CSV or JSON to run your
        scenario once per data row.
      </p>

      <div className={styles.sections}>
        {/* Scenario Selector */}
        <div className={styles.scenarioSelect}>
          <span className={styles.scenarioLabel}>Scenario</span>
          <select
            className={styles.select}
            value={selectedScenarioId ?? ''}
            onChange={(e) => setSelectedScenarioId(e.target.value ? Number(e.target.value) : null)}
          >
            <option value="">Select a scenario...</option>
            {scenarios.map((s) => (
              <option key={s.id} value={s.id}>
                {s.name} ({s.steps.length} steps)
              </option>
            ))}
          </select>
        </div>

        {/* Data Upload */}
        <DataUpload
          format={format}
          onFormatChange={setFormat}
          content={dataContent}
          onContentChange={setDataContent}
        />

        {/* Data Preview */}
        <DataPreviewTable rows={previewRows} />

        {/* Assertions */}
        <AssertionBuilder assertions={assertions} onChange={setAssertions} />

        {/* Execute Button */}
        <div className={styles.actions}>
          <button
            className={styles.executeBtn}
            onClick={handleExecute}
            disabled={!selectedScenarioId || !dataContent.trim() || loading}
            type="button"
          >
            {loading ? <Loader size={16} className="spin" /> : <Play size={16} />}
            {loading ? 'Executing...' : 'Execute'}
          </button>
        </div>

        {/* Error */}
        {error && (
          <div style={{ color: 'var(--storm-danger)', fontSize: 'var(--storm-text-sm)' }}>
            ⚠️ {error}
          </div>
        )}

        {/* Results */}
        {result && (
          <div className={styles.resultsContainer}>
            <div className={styles.resultsHeader}>
              <span className={styles.resultsTitle}>Results — {result.scenarioName}</span>
              <div className={styles.resultsSummary}>
                <span className={styles.summaryItem}>
                  Total:
                  <span className={styles.summaryValue}>{result.totalRows}</span>
                </span>
                <span className={styles.summaryItem}>
                  Passed:
                  <span className={`${styles.summaryValue} ${styles.summaryPass}`}>
                    {result.passedRows}
                  </span>
                </span>
                <span className={styles.summaryItem}>
                  Failed:
                  <span className={`${styles.summaryValue} ${styles.summaryFail}`}>
                    {result.failedRows}
                  </span>
                </span>
                <span className={styles.summaryItem}>
                  Duration:
                  <span className={styles.summaryValue}>{result.totalDurationMs}ms</span>
                </span>
              </div>
            </div>

            <div className={styles.rowList}>
              {result.rowResults.map((row) => (
                <div key={row.rowIndex} className={styles.rowItem}>
                  <span className={styles.rowIndex}>#{row.rowIndex}</span>
                  <span
                    className={`${styles.rowBadge} ${
                      row.allPassed ? styles.rowBadgePass : styles.rowBadgeFail
                    }`}
                  >
                    {row.allPassed ? (
                      <><CheckCircle size={12} /> Pass</>
                    ) : (
                      <><XCircle size={12} /> Fail</>
                    )}
                  </span>
                  <span className={styles.rowData}>
                    {Object.entries(row.rowData)
                      .map(([k, v]) => `${k}=${v}`)
                      .join(', ')}
                  </span>
                  <span className={styles.rowDuration}>
                    {row.result.totalDurationMs}ms
                  </span>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default DataDrivenPage;
