import { useState, useCallback, useMemo } from 'react';
import { useParams, Link } from 'react-router-dom';
import {
  ArrowLeft,
  Plus,
  ChevronDown,
  ChevronRight,
  Trash2,
  Play,
  Save,
  X,
} from 'lucide-react';
import {
  createScenario,
  getScenario,
  updateScenario,
  addStep,
  deleteStep as deleteStepApi,
  executeScenario,
} from '../api/scenarioApi';
import { ROUTES } from '../utils/constants';
import { HttpMethod } from '../types/test';
import type { KeyValuePair } from '../types/collection';
import type {
  ExtractionRule,
  ScenarioStep,
  StepExecutionResult,
  ScenarioExecutionResponse,
} from '../types/scenario';
import styles from './ScenarioBuilderPage.module.css';
import { useEffect } from 'react';

const HTTP_METHODS = Object.values(HttpMethod);

const METHOD_CLASS: Partial<Record<HttpMethod, string>> = {
  [HttpMethod.GET]: styles.methodGet,
  [HttpMethod.POST]: styles.methodPost,
  [HttpMethod.PUT]: styles.methodPut,
  [HttpMethod.DELETE]: styles.methodDelete,
  [HttpMethod.PATCH]: styles.methodPatch,
};

interface LocalStep {
  id?: number;
  name: string;
  url: string;
  method: HttpMethod;
  headers: KeyValuePair[];
  body: string;
  extractionRules: ExtractionRule[];
}

const EMPTY_STEP: LocalStep = {
  name: '',
  url: '',
  method: HttpMethod.GET,
  headers: [],
  body: '',
  extractionRules: [],
};

export function ScenarioBuilderPage() {
  const { id } = useParams<{ id: string }>();
  const isEditing = !!id;

  const [scenarioId, setScenarioId] = useState<number | null>(
    id ? Number(id) : null,
  );
  const [scenarioName, setScenarioName] = useState('New Scenario');
  const [description, setDescription] = useState('');
  const [failFast, setFailFast] = useState(true);
  const [steps, setSteps] = useState<LocalStep[]>([]);
  const [expandedStep, setExpandedStep] = useState<number | null>(null);
  const [saving, setSaving] = useState(false);
  const [running, setRunning] = useState(false);
  const [executionResult, setExecutionResult] =
    useState<ScenarioExecutionResponse | null>(null);

  // Load existing scenario if editing
  useEffect(() => {
    if (!isEditing || !id) return;
    getScenario(Number(id)).then((s) => {
      setScenarioId(s.id);
      setScenarioName(s.name);
      setDescription(s.description ?? '');
      setFailFast(s.failFast);
      setSteps(
        s.steps.map((st: ScenarioStep) => ({
          id: st.id,
          name: st.name,
          url: st.url,
          method: st.method,
          headers: st.headers ?? [],
          body: st.body ?? '',
          extractionRules: st.extractionRules ?? [],
        })),
      );
    });
  }, [isEditing, id]);

  // ── Step management ─────────────────────────────────────

  const addBlankStep = () => {
    setSteps((prev) => [...prev, { ...EMPTY_STEP }]);
    setExpandedStep(steps.length);
  };

  const updateLocalStep = useCallback(
    (index: number, updates: Partial<LocalStep>) => {
      setSteps((prev) =>
        prev.map((s, i) => (i === index ? { ...s, ...updates } : s)),
      );
    },
    [],
  );

  const removeStep = (index: number) => {
    setSteps((prev) => prev.filter((_, i) => i !== index));
    setExpandedStep(null);
  };

  // ── Header management within a step ─────────────────────

  const addStepHeader = (stepIndex: number) => {
    const step = steps[stepIndex];
    if (!step) return;
    updateLocalStep(stepIndex, {
      headers: [...step.headers, { key: '', value: '' }],
    });
  };

  const updateStepHeader = (
    stepIndex: number,
    headerIndex: number,
    field: 'key' | 'value',
    val: string,
  ) => {
    const step = steps[stepIndex];
    if (!step) return;
    const newHeaders = step.headers.map((h, i) =>
      i === headerIndex ? { ...h, [field]: val } : h,
    );
    updateLocalStep(stepIndex, { headers: newHeaders });
  };

  const removeStepHeader = (stepIndex: number, headerIndex: number) => {
    const step = steps[stepIndex];
    if (!step) return;
    updateLocalStep(stepIndex, {
      headers: step.headers.filter((_, i) => i !== headerIndex),
    });
  };

  // ── Extraction rules management ─────────────────────────

  const addExtractionRule = (stepIndex: number) => {
    const step = steps[stepIndex];
    if (!step) return;
    updateLocalStep(stepIndex, {
      extractionRules: [
        ...step.extractionRules,
        { variableName: '', jsonPath: '' },
      ],
    });
  };

  const updateExtractionRule = (
    stepIndex: number,
    ruleIndex: number,
    field: 'variableName' | 'jsonPath',
    val: string,
  ) => {
    const step = steps[stepIndex];
    if (!step) return;
    const newRules = step.extractionRules.map((r, i) =>
      i === ruleIndex ? { ...r, [field]: val } : r,
    );
    updateLocalStep(stepIndex, { extractionRules: newRules });
  };

  const removeExtractionRule = (stepIndex: number, ruleIndex: number) => {
    const step = steps[stepIndex];
    if (!step) return;
    updateLocalStep(stepIndex, {
      extractionRules: step.extractionRules.filter(
        (_, i) => i !== ruleIndex,
      ),
    });
  };

  // ── Variable flow analysis ──────────────────────────────

  const variableFlow = useMemo(() => {
    const flow: Array<{
      stepIndex: number;
      stepName: string;
      produces: string[];
      consumes: string[];
    }> = [];
    const availableVars = new Set<string>();

    for (let i = 0; i < steps.length; i++) {
      const step = steps[i];
      if (!step) continue;
      // Find consumed variables ({{varName}} in url, body, header values)
      const allText = [
        step.url,
        step.body,
        ...step.headers.map((h) => h.value),
      ].join(' ');
      const consumed: string[] = [];
      const regex = /\{\{(\w+)}}/g;
      let match;
      while ((match = regex.exec(allText)) !== null) {
        if (match[1]) consumed.push(match[1]);
      }

      // Produced variables
      const produces = step.extractionRules
        .filter((r) => r.variableName.trim())
        .map((r) => r.variableName);

      flow.push({
        stepIndex: i,
        stepName: step.name || `Step ${i + 1}`,
        produces,
        consumes: consumed.filter((v) => !availableVars.has(v)),
      });

      // Add produced vars to available set
      produces.forEach((v) => availableVars.add(v));
    }

    return flow;
  }, [steps]);

  // ── Save scenario ───────────────────────────────────────

  const handleSave = async (): Promise<number | null> => {
    if (!scenarioName.trim()) return null;
    setSaving(true);
    try {
      let sid = scenarioId;
      if (sid) {
        await updateScenario(sid, {
          name: scenarioName.trim(),
          description: description.trim() || undefined,
          failFast,
        });
        // Delete then re-add steps for simplicity
        const existing = await getScenario(sid);
        for (const s of existing.steps) {
          await deleteStepApi(sid, s.id);
        }
      } else {
        const created = await createScenario({
          name: scenarioName.trim(),
          description: description.trim() || undefined,
          failFast,
        });
        sid = created.id;
        setScenarioId(sid);
      }

      if (!sid) return null;

      // Add all steps
      for (const step of steps) {
        const saved = await addStep(sid, {
          name: step.name || 'Untitled Step',
          url: step.url,
          method: step.method,
          headers: step.headers.filter((h) => h.key.trim()),
          body: step.body || undefined,
          extractionRules: step.extractionRules.filter(
            (r) => r.variableName.trim() && r.jsonPath.trim(),
          ),
        });
        // Update local step with server ID
        step.id = saved.id;
      }

      return sid;
    } finally {
      setSaving(false);
    }
  };

  // ── Execute scenario ────────────────────────────────────

  const handleRun = async () => {
    setRunning(true);
    setExecutionResult(null);
    try {
      const sid = await handleSave();
      if (!sid) return;
      const result = await executeScenario(sid);
      setExecutionResult(result);
    } finally {
      setRunning(false);
    }
  };

  // ── Result helpers ──────────────────────────────────────

  const getStepResult = (
    stepOrder: number,
  ): StepExecutionResult | undefined => {
    return executionResult?.stepResults.find(
      (r) => r.stepOrder === stepOrder,
    );
  };

  // ── Render ──────────────────────────────────────────────

  return (
    <div className={styles.container}>
      <Link to={ROUTES.SCENARIOS} className={styles.backLink}>
        <ArrowLeft size={14} />
        Back to Scenarios
      </Link>

      {/* Top Bar */}
      <div className={styles.topBar}>
        <div className={styles.topBarLeft}>
          <input
            className={styles.scenarioName}
            value={scenarioName}
            onChange={(e) => setScenarioName(e.target.value)}
            placeholder="Scenario Name"
          />
          <label className={styles.failFastToggle}>
            <input
              type="checkbox"
              checked={failFast}
              onChange={(e) => setFailFast(e.target.checked)}
            />
            Fail fast
          </label>
        </div>
        <div className={styles.topBarActions}>
          <button
            className={styles.btnSecondary}
            onClick={handleSave}
            disabled={saving || running}
          >
            <Save size={14} />
            {saving ? 'Saving…' : 'Save'}
          </button>
          <button
            className={styles.btnRun}
            onClick={handleRun}
            disabled={saving || running || steps.length === 0}
          >
            <Play size={14} />
            {running ? 'Running…' : 'Run'}
          </button>
        </div>
      </div>

      {/* Execution Results Summary */}
      {executionResult && (
        <div
          className={
            executionResult.success
              ? styles.resultsSummarySuccess
              : styles.resultsSummaryFailed
          }
        >
          <span>
            {executionResult.success ? '✓ All steps passed' : '✗ Scenario failed'}
            {' — '}
            {executionResult.passedSteps}/{executionResult.totalSteps} passed
          </span>
          <span>{executionResult.totalDurationMs}ms total</span>
        </div>
      )}

      <div className={styles.layout}>
        {/* ── Step List (Left) ──────────────────────────── */}
        <div className={styles.stepList}>
          {steps.map((step, index) => {
            const isExpanded = expandedStep === index;
            const result = getStepResult(index);
            const cardClass = [
              styles.stepCard,
              result?.success === true ? styles.stepCardSuccess : '',
              result?.success === false ? styles.stepCardFailed : '',
            ]
              .filter(Boolean)
              .join(' ');

            return (
              <div key={index} className={cardClass}>
                {/* Step Header */}
                <div
                  className={styles.stepHeader}
                  onClick={() =>
                    setExpandedStep(isExpanded ? null : index)
                  }
                >
                  <span className={styles.stepNumber}>{index + 1}</span>
                  <span
                    className={`${styles.stepMethodBadge} ${METHOD_CLASS[step.method] ?? ''}`}
                  >
                    {step.method}
                  </span>
                  <span className={styles.stepName}>
                    {step.name || 'Untitled Step'}
                  </span>

                  {/* Result badges */}
                  {result && (
                    <span
                      className={
                        result.success
                          ? styles.stepResultPass
                          : styles.stepResultFail
                      }
                    >
                      {result.success ? 'PASS' : 'FAIL'}
                    </span>
                  )}
                  {result && (
                    <span
                      style={{
                        fontSize: 'var(--storm-text-xs)',
                        color: 'var(--storm-text-tertiary)',
                      }}
                    >
                      {Math.round(result.responseTimeMs)}ms
                    </span>
                  )}

                  <div className={styles.stepActions}>
                    <button
                      className={`${styles.stepActionBtn} ${styles.stepActionBtnDanger}`}
                      onClick={(e) => {
                        e.stopPropagation();
                        removeStep(index);
                      }}
                      title="Remove step"
                    >
                      <Trash2 size={14} />
                    </button>
                    {isExpanded ? (
                      <ChevronDown size={16} />
                    ) : (
                      <ChevronRight size={16} />
                    )}
                  </div>
                </div>

                {/* Step Body (expanded) */}
                {isExpanded && (
                  <div className={styles.stepBody}>
                    <div className={styles.formRow}>
                      <div className={styles.formGroup}>
                        <label className={styles.formLabel}>URL</label>
                        <input
                          className={styles.formInput}
                          value={step.url}
                          onChange={(e) =>
                            updateLocalStep(index, { url: e.target.value })
                          }
                          placeholder="https://api.example.com/{{userId}}"
                        />
                      </div>
                      <div className={styles.formGroup}>
                        <label className={styles.formLabel}>Method</label>
                        <select
                          className={styles.formSelect}
                          value={step.method}
                          onChange={(e) =>
                            updateLocalStep(index, {
                              method: e.target.value as HttpMethod,
                            })
                          }
                        >
                          {HTTP_METHODS.map((m) => (
                            <option key={m}>{m}</option>
                          ))}
                        </select>
                      </div>
                    </div>

                    <div className={styles.formGroup}>
                      <label className={styles.formLabel}>Name</label>
                      <input
                        className={styles.formInput}
                        value={step.name}
                        onChange={(e) =>
                          updateLocalStep(index, { name: e.target.value })
                        }
                        placeholder="e.g. Create User"
                        style={{ fontFamily: 'inherit' }}
                      />
                    </div>

                    {/* Headers */}
                    <div className={styles.formGroup}>
                      <label className={styles.formLabel}>Headers</label>
                      {step.headers.map((h, hi) => (
                        <div key={hi} className={styles.headerRow}>
                          <input
                            className={styles.headerInput}
                            placeholder="Key"
                            value={h.key}
                            onChange={(e) =>
                              updateStepHeader(index, hi, 'key', e.target.value)
                            }
                          />
                          <input
                            className={styles.headerInput}
                            placeholder="Value (supports {{var}})"
                            value={h.value}
                            onChange={(e) =>
                              updateStepHeader(
                                index,
                                hi,
                                'value',
                                e.target.value,
                              )
                            }
                          />
                          <button
                            className={styles.removeBtn}
                            onClick={() => removeStepHeader(index, hi)}
                          >
                            <X size={14} />
                          </button>
                        </div>
                      ))}
                      <button
                        className={styles.addRuleBtn}
                        onClick={() => addStepHeader(index)}
                      >
                        + Add Header
                      </button>
                    </div>

                    {/* Body */}
                    <div className={styles.formGroup}>
                      <label className={styles.formLabel}>
                        Body (supports {'{{var}}'})
                      </label>
                      <textarea
                        className={styles.formTextarea}
                        value={step.body}
                        onChange={(e) =>
                          updateLocalStep(index, { body: e.target.value })
                        }
                        placeholder='{"name": "{{userName}}"}'
                      />
                    </div>

                    {/* Extraction Rules */}
                    <div className={styles.extractionSection}>
                      <label className={styles.formLabel}>
                        Variable Extraction Rules
                      </label>
                      {step.extractionRules.map((rule, ri) => (
                        <div key={ri} className={styles.extractionRow}>
                          <input
                            className={styles.extractionInput}
                            placeholder="Variable name (e.g. userId)"
                            value={rule.variableName}
                            onChange={(e) =>
                              updateExtractionRule(
                                index,
                                ri,
                                'variableName',
                                e.target.value,
                              )
                            }
                          />
                          <input
                            className={styles.extractionInput}
                            placeholder="JSON path (e.g. $.data.id)"
                            value={rule.jsonPath}
                            onChange={(e) =>
                              updateExtractionRule(
                                index,
                                ri,
                                'jsonPath',
                                e.target.value,
                              )
                            }
                          />
                          <button
                            className={styles.removeBtn}
                            onClick={() =>
                              removeExtractionRule(index, ri)
                            }
                          >
                            <X size={14} />
                          </button>
                        </div>
                      ))}
                      <button
                        className={styles.addRuleBtn}
                        onClick={() => addExtractionRule(index)}
                      >
                        + Add Extraction Rule
                      </button>
                    </div>

                    {/* Step execution result detail */}
                    {result && (
                      <>
                        <div className={styles.stepResultMeta}>
                          <span
                            className={
                              result.success
                                ? styles.stepResultPass
                                : styles.stepResultFail
                            }
                          >
                            {result.statusCode || 'ERR'}
                          </span>
                          <span>
                            {Math.round(result.responseTimeMs)}ms
                          </span>
                          {result.errorMessage && (
                            <span style={{ color: 'var(--storm-danger)' }}>
                              {result.errorMessage}
                            </span>
                          )}
                        </div>

                        {result.responseBodyPreview && (
                          <div className={styles.responsePreview}>
                            {result.responseBodyPreview}
                          </div>
                        )}

                        {Object.keys(result.extractedVariables).length >
                          0 && (
                          <div className={styles.extractedVars}>
                            {Object.entries(
                              result.extractedVariables,
                            ).map(([k, v]) => (
                              <span
                                key={k}
                                className={styles.flowVarProduced}
                              >
                                {k}={v}
                              </span>
                            ))}
                          </div>
                        )}
                      </>
                    )}
                  </div>
                )}
              </div>
            );
          })}

          {/* Add Step Button */}
          <div className={styles.addStepArea}>
            <button className={styles.addStepBtn} onClick={addBlankStep}>
              <Plus size={14} />
              Add Step
            </button>
          </div>
        </div>

        {/* ── Variable Flow Panel (Right) ──────────────── */}
        <div className={styles.variablePanel}>
          <h3 className={styles.panelTitle}>Variable Flow</h3>

          {variableFlow.length === 0 ? (
            <p className={styles.flowEmpty}>
              Add steps to see variable flow.
            </p>
          ) : (
            variableFlow.map((flow) => (
              <div key={flow.stepIndex} className={styles.flowStep}>
                <div className={styles.flowStepLabel}>
                  Step {flow.stepIndex + 1}: {flow.stepName}
                </div>

                {flow.consumes.length > 0 && (
                  <div className={styles.flowProduces}>
                    {flow.consumes.map((v) => (
                      <span key={v} className={styles.flowVarConsumed}>
                        ← {v}
                      </span>
                    ))}
                  </div>
                )}

                {flow.produces.length > 0 && (
                  <div className={styles.flowProduces}>
                    {flow.produces.map((v) => (
                      <span key={v} className={styles.flowVarProduced}>
                        → {v}
                      </span>
                    ))}
                  </div>
                )}

                {flow.consumes.length === 0 &&
                  flow.produces.length === 0 && (
                    <span className={styles.flowEmpty}>
                      No variables
                    </span>
                  )}
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
}
