import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Layers, Edit2, Trash2 } from 'lucide-react';
import { ROUTES } from '../../utils/constants';
import type { ApiCollection } from '../../types/collection';
import styles from './CollectionCard.module.css';

interface CollectionCardProps {
  collection: ApiCollection;
  onEdit: (collection: ApiCollection) => void;
  onDelete: (id: number) => void;
}

export const CollectionCard = React.memo(function CollectionCard({
  collection,
  onEdit,
  onDelete,
}: CollectionCardProps) {
  const navigate = useNavigate();

  const handleClick = () => {
    navigate(ROUTES.COLLECTION_DETAIL(collection.id));
  };

  const handleEdit = (e: React.MouseEvent) => {
    e.stopPropagation();
    onEdit(collection);
  };

  const handleDelete = (e: React.MouseEvent) => {
    e.stopPropagation();
    onDelete(collection.id);
  };

  const endpointCount = collection.endpoints?.length ?? 0;
  const createdDate = new Date(collection.createdAt).toLocaleDateString();

  return (
    <div
      className={styles.card}
      onClick={handleClick}
      role="button"
      tabIndex={0}
      onKeyDown={(e) => e.key === 'Enter' && handleClick()}
      aria-label={`Collection: ${collection.name}`}
    >
      <div className={styles.header}>
        <h3 className={styles.name}>{collection.name}</h3>
        <div className={styles.actions}>
          <button
            className={styles.actionBtn}
            onClick={handleEdit}
            title="Edit collection"
            aria-label="Edit collection"
          >
            <Edit2 size={14} />
          </button>
          <button
            className={`${styles.actionBtn} ${styles.actionBtnDanger}`}
            onClick={handleDelete}
            title="Delete collection"
            aria-label="Delete collection"
          >
            <Trash2 size={14} />
          </button>
        </div>
      </div>

      {collection.description && (
        <p className={styles.description}>{collection.description}</p>
      )}

      <div className={styles.footer}>
        <span className={styles.badge}>
          <Layers size={12} />
          {endpointCount} endpoint{endpointCount !== 1 ? 's' : ''}
        </span>
        <span className={styles.date}>{createdDate}</span>
      </div>
    </div>
  );
});
