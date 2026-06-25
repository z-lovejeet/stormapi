import { useState, useCallback } from 'react';
import { Plus, Trash2 } from 'lucide-react';
import { Input } from '../common/Input';
import { Button } from '../common/Button';
import styles from './HeadersEditor.module.css';

interface HeaderRow {
  key: string;
  value: string;
}

interface HeadersEditorProps {
  value: Record<string, string>;
  onChange: (headers: Record<string, string>) => void;
}

function toRows(record: Record<string, string>): HeaderRow[] {
  const entries = Object.entries(record);
  return entries.length > 0 ? entries.map(([key, value]) => ({ key, value })) : [];
}

function toRecord(rows: HeaderRow[]): Record<string, string> {
  const record: Record<string, string> = {};
  for (const row of rows) {
    if (row.key.trim()) {
      record[row.key.trim()] = row.value;
    }
  }
  return record;
}

export function HeadersEditor({ value, onChange }: HeadersEditorProps) {
  const [rows, setRows] = useState<HeaderRow[]>(() => toRows(value));

  const sync = useCallback(
    (updated: HeaderRow[]) => {
      setRows(updated);
      onChange(toRecord(updated));
    },
    [onChange],
  );

  const addRow = () => sync([...rows, { key: '', value: '' }]);

  const removeRow = (index: number) => sync(rows.filter((_, i) => i !== index));

  const updateRow = (index: number, field: 'key' | 'value', val: string) => {
    const updated = rows.map((row, i) => (i === index ? { ...row, [field]: val } : row));
    sync(updated);
  };

  return (
    <div className={styles.container}>
      <label
        style={{
          fontSize: 'var(--storm-text-sm)',
          fontWeight: 'var(--storm-weight-medium)',
          color: 'var(--storm-text-primary)',
        }}
      >
        Headers
      </label>
      {rows.map((row, i) => (
        <div className={styles.row} key={i}>
          <Input
            placeholder="Header name"
            value={row.key}
            onChange={(e) => updateRow(i, 'key', e.target.value)}
            aria-label={`Header ${i + 1} name`}
          />
          <Input
            placeholder="Header value"
            value={row.value}
            onChange={(e) => updateRow(i, 'value', e.target.value)}
            aria-label={`Header ${i + 1} value`}
          />
          <Button
            variant="ghost"
            size="sm"
            icon={Trash2}
            iconOnly
            aria-label={`Remove header ${i + 1}`}
            onClick={() => removeRow(i)}
            type="button"
          />
        </div>
      ))}
      <div className={styles.addButton}>
        <Button variant="ghost" size="sm" icon={Plus} onClick={addRow} type="button">
          Add Header
        </Button>
      </div>
    </div>
  );
}
