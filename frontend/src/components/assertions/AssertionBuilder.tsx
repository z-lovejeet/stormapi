import React, { memo, useCallback } from 'react';
import { Shield, Plus, X } from 'lucide-react';
import type { AssertionDefinition, AssertionType } from '../../types/data';
import styles from './AssertionBuilder.module.css';

interface AssertionBuilderProps {
  assertions: AssertionDefinition[];
  onChange: (assertions: AssertionDefinition[]) => void;
}

const ASSERTION_TYPES: { value: AssertionType; label: string; needsTarget: boolean }[] = [
  { value: 'STATUS_CODE', label: 'Status Code', needsTarget: false },
  { value: 'RESPONSE_TIME', label: 'Response Time (ms)', needsTarget: false },
  { value: 'BODY_CONTAINS', label: 'Body Contains', needsTarget: false },
  { value: 'JSON_PATH', label: 'JSON Path', needsTarget: true },
  { value: 'HEADER', label: 'Header', needsTarget: true },
];

/**
 * Inline assertion builder — lets users add/edit/remove assertions per step.
 * Assertion definitions are passed up to the parent via onChange.
 */
const AssertionBuilder: React.FC<AssertionBuilderProps> = ({ assertions, onChange }) => {
  const handleAdd = useCallback(() => {
    onChange([
      ...assertions,
      { type: 'STATUS_CODE', expectedValue: '200' },
    ]);
  }, [assertions, onChange]);

  const handleRemove = useCallback(
    (index: number) => {
      onChange(assertions.filter((_, i) => i !== index));
    },
    [assertions, onChange],
  );

  const handleUpdate = useCallback(
    (index: number, field: keyof AssertionDefinition, value: string) => {
      const updated = assertions.map((a, i) => {
        if (i !== index) return a;
        return { ...a, [field]: value };
      });
      onChange(updated);
    },
    [assertions, onChange],
  );

  const getTypeConfig = (type: AssertionType) =>
    ASSERTION_TYPES.find((t) => t.value === type);

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <span className={styles.title}>
          <Shield size={14} />
          Assertions ({assertions.length})
        </span>
        <button className={styles.addBtn} onClick={handleAdd} type="button">
          <Plus size={12} /> Add
        </button>
      </div>

      {assertions.length === 0 ? (
        <div className={styles.empty}>No assertions defined. Click "Add" to create one.</div>
      ) : (
        <div className={styles.list}>
          {assertions.map((assertion, index) => {
            const config = getTypeConfig(assertion.type);
            return (
              <div key={index} className={styles.row}>
                <select
                  className={styles.select}
                  value={assertion.type}
                  onChange={(e) => handleUpdate(index, 'type', e.target.value)}
                >
                  {ASSERTION_TYPES.map((t) => (
                    <option key={t.value} value={t.value}>
                      {t.label}
                    </option>
                  ))}
                </select>

                {config?.needsTarget ? (
                  <input
                    className={styles.input}
                    placeholder={assertion.type === 'JSON_PATH' ? '$.data.id' : 'Header-Name'}
                    value={assertion.target || ''}
                    onChange={(e) => handleUpdate(index, 'target', e.target.value)}
                  />
                ) : (
                  <div />
                )}

                <input
                  className={styles.input}
                  placeholder={
                    assertion.type === 'RESPONSE_TIME'
                      ? 'Max ms (e.g. 500)'
                      : 'Expected value'
                  }
                  value={assertion.expectedValue}
                  onChange={(e) => handleUpdate(index, 'expectedValue', e.target.value)}
                />

                <button
                  className={styles.removeBtn}
                  onClick={() => handleRemove(index)}
                  type="button"
                  title="Remove assertion"
                >
                  <X size={14} />
                </button>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};

export default memo(AssertionBuilder);
