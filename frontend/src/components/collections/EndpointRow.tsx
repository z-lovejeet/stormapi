import React from 'react';
import { Edit2, Trash2, GripVertical, Workflow } from 'lucide-react';
import { HttpMethod } from '../../types/test';
import type { ApiEndpoint } from '../../types/collection';
import styles from './EndpointRow.module.css';

interface EndpointRowProps {
  endpoint: ApiEndpoint;
  onEdit: (endpoint: ApiEndpoint) => void;
  onDelete: (endpointId: number) => void;
  onUseInScenario?: (endpoint: ApiEndpoint) => void;
  draggable?: boolean;
  onDragStart?: (e: React.DragEvent, index: number) => void;
  onDragOver?: (e: React.DragEvent) => void;
  onDrop?: (e: React.DragEvent, index: number) => void;
  index?: number;
}

const METHOD_CLASS: Partial<Record<HttpMethod, string>> = {
  [HttpMethod.GET]: styles.methodGet,
  [HttpMethod.POST]: styles.methodPost,
  [HttpMethod.PUT]: styles.methodPut,
  [HttpMethod.DELETE]: styles.methodDelete,
  [HttpMethod.PATCH]: styles.methodPatch,
};

export const EndpointRow = React.memo(function EndpointRow({
  endpoint,
  onEdit,
  onDelete,
  onUseInScenario,
  draggable = false,
  onDragStart,
  onDragOver,
  onDrop,
  index = 0,
}: EndpointRowProps) {
  return (
    <div
      className={styles.row}
      draggable={draggable}
      onDragStart={draggable && onDragStart ? (e) => onDragStart(e, index) : undefined}
      onDragOver={draggable && onDragOver ? (e) => onDragOver(e) : undefined}
      onDrop={draggable && onDrop ? (e) => onDrop(e, index) : undefined}
    >
      {draggable && (
        <span className={styles.dragHandle} title="Drag to reorder">
          <GripVertical size={14} />
        </span>
      )}

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
        {onUseInScenario && (
          <button
            className={`${styles.actionBtn} ${styles.actionBtnScenario}`}
            onClick={() => onUseInScenario(endpoint)}
            title="Use in Scenario"
            aria-label={`Use ${endpoint.name} in scenario`}
          >
            <Workflow size={14} />
          </button>
        )}
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
