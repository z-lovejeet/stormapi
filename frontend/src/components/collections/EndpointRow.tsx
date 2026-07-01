import React from 'react';
import { Edit2, Trash2 } from 'lucide-react';
import type { ApiEndpoint } from '../../types/collection';
import styles from './EndpointRow.module.css';

interface EndpointRowProps {
  endpoint: ApiEndpoint;
  onEdit: (endpoint: ApiEndpoint) => void;
  onDelete: (endpointId: number) => void;
}

const METHOD_CLASS: Record<string, string> = {
  GET: styles.methodGet,
  POST: styles.methodPost,
  PUT: styles.methodPut,
  DELETE: styles.methodDelete,
  PATCH: styles.methodPatch,
};

export const EndpointRow = React.memo(function EndpointRow({
  endpoint,
  onEdit,
  onDelete,
}: EndpointRowProps) {
  return (
    <div className={styles.row}>
      <span
        className={`${styles.methodBadge} ${METHOD_CLASS[endpoint.method] ?? ''}`}
      >
        {endpoint.method}
      </span>

      <div className={styles.info}>
        <p className={styles.name}>{endpoint.name}</p>
        <p className={styles.url}>{endpoint.url}</p>
      </div>

      <div className={styles.actions}>
        <button
          className={styles.actionBtn}
          onClick={() => onEdit(endpoint)}
          title="Edit endpoint"
          aria-label={`Edit ${endpoint.name}`}
        >
          <Edit2 size={14} />
        </button>
        <button
          className={`${styles.actionBtn} ${styles.actionBtnDanger}`}
          onClick={() => onDelete(endpoint.id)}
          title="Delete endpoint"
          aria-label={`Delete ${endpoint.name}`}
        >
          <Trash2 size={14} />
        </button>
      </div>
    </div>
  );
});
