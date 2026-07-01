import { useState, useEffect, useCallback } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Plus, Workflow, Layers, Edit2, Trash2, ShieldAlert } from 'lucide-react';
import {
  listScenarios,
  deleteScenario,
} from '../api/scenarioApi';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { EmptyState } from '../components/common/EmptyState';
import { ROUTES } from '../utils/constants';
import type { TestScenario } from '../types/scenario';
import styles from './ScenariosPage.module.css';

export function ScenariosPage() {
  const navigate = useNavigate();
  const [scenarios, setScenarios] = useState<TestScenario[]>([]);
  const [loading, setLoading] = useState(true);
  const [deleteTarget, setDeleteTarget] = useState<number | null>(null);

  const fetchScenarios = useCallback(async () => {
    setLoading(true);
    try {
      const data = await listScenarios();
      setScenarios(data);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchScenarios();
  }, [fetchScenarios]);

  const handleDelete = async () => {
    if (deleteTarget == null) return;
    await deleteScenario(deleteTarget);
    setDeleteTarget(null);
    await fetchScenarios();
  };

  if (loading) return <LoadingSpinner />;

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <h1 className={styles.title}>Test Scenarios</h1>
        <Link to={ROUTES.SCENARIO_BUILDER} className={styles.createBtn}>
          <Plus size={14} />
          New Scenario
        </Link>
      </div>

      {scenarios.length === 0 ? (
        <EmptyState
          icon={Workflow}
          title="No Scenarios"
          description="Create multi-step test scenarios with variable chaining."
        />
      ) : (
        <div className={styles.grid}>
          {scenarios.map((scenario) => (
            <div
              key={scenario.id}
              className={styles.card}
              onClick={() =>
                navigate(ROUTES.SCENARIO_EDIT(scenario.id))
              }
              role="button"
              tabIndex={0}
              onKeyDown={(e) =>
                e.key === 'Enter' &&
                navigate(ROUTES.SCENARIO_EDIT(scenario.id))
              }
            >
              <div className={styles.cardHeader}>
                <h3 className={styles.cardName}>{scenario.name}</h3>
                <div className={styles.actions}>
                  <button
                    className={styles.actionBtn}
                    onClick={(e) => {
                      e.stopPropagation();
                      navigate(ROUTES.SCENARIO_EDIT(scenario.id));
                    }}
                    title="Edit scenario"
                  >
                    <Edit2 size={14} />
                  </button>
                  <button
                    className={`${styles.actionBtn} ${styles.actionBtnDanger}`}
                    onClick={(e) => {
                      e.stopPropagation();
                      setDeleteTarget(scenario.id);
                    }}
                    title="Delete scenario"
                  >
                    <Trash2 size={14} />
                  </button>
                </div>
              </div>

              {scenario.description && (
                <p className={styles.cardDesc}>{scenario.description}</p>
              )}

              <div className={styles.cardFooter}>
                <div style={{ display: 'flex', gap: '0.5rem' }}>
                  <span className={styles.badge}>
                    <Layers size={12} />
                    {scenario.steps?.length ?? 0} step
                    {(scenario.steps?.length ?? 0) !== 1 ? 's' : ''}
                  </span>
                  {scenario.failFast && (
                    <span className={styles.failFastBadge}>
                      <ShieldAlert size={12} />
                      Fail fast
                    </span>
                  )}
                </div>
                <span className={styles.date}>
                  {new Date(scenario.createdAt).toLocaleDateString()}
                </span>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Delete Confirmation */}
      {deleteTarget != null && (
        <div
          className={styles.modalOverlay}
          onClick={() => setDeleteTarget(null)}
        >
          <div
            className={styles.modal}
            onClick={(e) => e.stopPropagation()}
          >
            <h2 className={styles.modalTitle}>Delete Scenario?</h2>
            <p className={styles.confirmText}>
              This will permanently delete this scenario and all its steps.
            </p>
            <div className={styles.modalActions}>
              <button
                className={styles.cancelBtn}
                onClick={() => setDeleteTarget(null)}
              >
                Cancel
              </button>
              <button
                className={styles.confirmDanger}
                onClick={handleDelete}
              >
                Delete
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
