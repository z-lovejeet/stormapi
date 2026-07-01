import { useState, useMemo } from 'react';
import { Plus, FolderOpen } from 'lucide-react';
import { useCollections } from '../hooks/useCollections';
import { CollectionCard } from '../components/collections/CollectionCard';
import { EmptyState } from '../components/common/EmptyState';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import type { ApiCollection } from '../types/collection';
import styles from './CollectionsPage.module.css';

export function CollectionsPage() {
  const { collections, loading, error, create, update, remove } =
    useCollections();
  const [search, setSearch] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [editTarget, setEditTarget] = useState<ApiCollection | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<number | null>(null);
  const [formName, setFormName] = useState('');
  const [formDesc, setFormDesc] = useState('');
  const [saving, setSaving] = useState(false);

  const filtered = useMemo(() => {
    if (!search.trim()) return collections;
    const q = search.toLowerCase();
    return collections.filter(
      (c) =>
        c.name.toLowerCase().includes(q) ||
        c.description?.toLowerCase().includes(q),
    );
  }, [collections, search]);

  // ── Modal handlers ──────────────────────────────────────

  const openCreateModal = () => {
    setEditTarget(null);
    setFormName('');
    setFormDesc('');
    setModalOpen(true);
  };

  const openEditModal = (collection: ApiCollection) => {
    setEditTarget(collection);
    setFormName(collection.name);
    setFormDesc(collection.description ?? '');
    setModalOpen(true);
  };

  const closeModal = () => {
    setModalOpen(false);
    setEditTarget(null);
  };

  const handleSave = async () => {
    if (!formName.trim()) return;
    setSaving(true);
    try {
      if (editTarget) {
        await update(editTarget.id, formName.trim(), formDesc.trim() || undefined);
      } else {
        await create(formName.trim(), formDesc.trim() || undefined);
      }
      closeModal();
    } finally {
      setSaving(false);
    }
  };

  // ── Delete handlers ─────────────────────────────────────

  const handleDeleteConfirm = async () => {
    if (deleteTarget == null) return;
    await remove(deleteTarget);
    setDeleteTarget(null);
  };

  // ── Render ──────────────────────────────────────────────

  if (loading) return <LoadingSpinner />;

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <h1 className={styles.title}>API Collections</h1>
        <div className={styles.headerActions}>
          <input
            className={styles.searchInput}
            placeholder="Search collections…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            aria-label="Search collections"
          />
          <button
            className={styles.createBtn}
            onClick={openCreateModal}
            aria-label="Create new collection"
          >
            <Plus size={14} />
            New Collection
          </button>
        </div>
      </div>

      {error && <div className={styles.errorBanner}>{error}</div>}

      {filtered.length === 0 ? (
        <EmptyState
          icon={FolderOpen}
          title="No Collections"
          description={
            search
              ? 'No collections match your search.'
              : 'Create your first collection to organize API endpoints.'
          }
        />
      ) : (
        <div className={styles.grid}>
          {filtered.map((collection) => (
            <CollectionCard
              key={collection.id}
              collection={collection}
              onEdit={openEditModal}
              onDelete={setDeleteTarget}
            />
          ))}
        </div>
      )}

      {/* ── Create/Edit Modal ────────────────────────────── */}
      {modalOpen && (
        <div className={styles.modalOverlay} onClick={closeModal}>
          <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
            <h2 className={styles.modalTitle}>
              {editTarget ? 'Edit Collection' : 'New Collection'}
            </h2>

            <div className={styles.formGroup}>
              <label className={styles.formLabel} htmlFor="col-name">
                Name <span style={{ color: 'var(--storm-danger)' }}>*</span>
              </label>
              <input
                id="col-name"
                className={styles.formInput}
                value={formName}
                onChange={(e) => setFormName(e.target.value)}
                placeholder="e.g. User API"
                autoFocus
              />
            </div>

            <div className={styles.formGroup}>
              <label className={styles.formLabel} htmlFor="col-desc">
                Description
              </label>
              <textarea
                id="col-desc"
                className={styles.formTextarea}
                value={formDesc}
                onChange={(e) => setFormDesc(e.target.value)}
                placeholder="Optional description…"
              />
            </div>

            <div className={styles.modalActions}>
              <button className={styles.cancelBtn} onClick={closeModal}>
                Cancel
              </button>
              <button
                className={styles.saveBtn}
                onClick={handleSave}
                disabled={!formName.trim() || saving}
              >
                {saving ? 'Saving…' : editTarget ? 'Update' : 'Create'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ── Delete Confirmation ──────────────────────────── */}
      {deleteTarget != null && (
        <div
          className={styles.modalOverlay}
          onClick={() => setDeleteTarget(null)}
        >
          <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
            <h2 className={styles.modalTitle}>Delete Collection?</h2>
            <p className={styles.confirmText}>
              This will permanently delete this collection and all its
              endpoints. This action cannot be undone.
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
                onClick={handleDeleteConfirm}
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
