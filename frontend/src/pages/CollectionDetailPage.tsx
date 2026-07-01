import { useState, useEffect, useCallback } from 'react';
import { useParams, Link } from 'react-router-dom';
import { ArrowLeft, Plus, Layers, X } from 'lucide-react';
import {
  getCollection,
  addEndpoint,
  updateEndpoint,
  deleteEndpoint,
} from '../api/collectionApi';
import { EndpointRow } from '../components/collections/EndpointRow';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { EmptyState } from '../components/common/EmptyState';
import { ROUTES } from '../utils/constants';
import type { ApiCollection, ApiEndpoint, KeyValuePair } from '../types/collection';
import type { HttpMethod } from '../types/test';
import styles from './CollectionDetailPage.module.css';

const HTTP_METHODS: HttpMethod[] = ['GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'HEAD', 'OPTIONS'];

interface EndpointForm {
  name: string;
  url: string;
  method: HttpMethod;
  headers: KeyValuePair[];
  body: string;
  description: string;
}

const EMPTY_FORM: EndpointForm = {
  name: '',
  url: '',
  method: 'GET',
  headers: [],
  body: '',
  description: '',
};

export function CollectionDetailPage() {
  const { id } = useParams<{ id: string }>();
  const collectionId = Number(id);

  const [collection, setCollection] = useState<ApiCollection | null>(null);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editEndpoint, setEditEndpoint] = useState<ApiEndpoint | null>(null);
  const [form, setForm] = useState<EndpointForm>(EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<number | null>(null);

  const fetchCollection = useCallback(async () => {
    setLoading(true);
    try {
      const data = await getCollection(collectionId);
      setCollection(data);
    } catch {
      setCollection(null);
    } finally {
      setLoading(false);
    }
  }, [collectionId]);

  useEffect(() => {
    fetchCollection();
  }, [fetchCollection]);

  // ── Modal handlers ──────────────────────────────────────

  const openAddModal = () => {
    setEditEndpoint(null);
    setForm(EMPTY_FORM);
    setModalOpen(true);
  };

  const openEditModal = (endpoint: ApiEndpoint) => {
    setEditEndpoint(endpoint);
    setForm({
      name: endpoint.name,
      url: endpoint.url,
      method: endpoint.method,
      headers: endpoint.headers ?? [],
      body: endpoint.body ?? '',
      description: endpoint.description ?? '',
    });
    setModalOpen(true);
  };

  const closeModal = () => {
    setModalOpen(false);
    setEditEndpoint(null);
  };

  const handleSave = async () => {
    if (!form.name.trim() || !form.url.trim()) return;
    setSaving(true);
    try {
      const payload = {
        name: form.name.trim(),
        url: form.url.trim(),
        method: form.method,
        headers: form.headers.filter((h) => h.key.trim()),
        body: form.body || undefined,
        description: form.description.trim() || undefined,
        sortOrder: editEndpoint?.sortOrder ?? 0,
      };
      if (editEndpoint) {
        await updateEndpoint(collectionId, editEndpoint.id, payload);
      } else {
        await addEndpoint(collectionId, payload);
      }
      closeModal();
      await fetchCollection();
    } finally {
      setSaving(false);
    }
  };

  // ── Header management ───────────────────────────────────

  const addHeader = () => {
    setForm((prev) => ({
      ...prev,
      headers: [...prev.headers, { key: '', value: '' }],
    }));
  };

  const updateHeader = (index: number, field: 'key' | 'value', val: string) => {
    setForm((prev) => ({
      ...prev,
      headers: prev.headers.map((h, i) =>
        i === index ? { ...h, [field]: val } : h,
      ),
    }));
  };

  const removeHeader = (index: number) => {
    setForm((prev) => ({
      ...prev,
      headers: prev.headers.filter((_, i) => i !== index),
    }));
  };

  // ── Delete ──────────────────────────────────────────────

  const handleDeleteConfirm = async () => {
    if (deleteTarget == null) return;
    await deleteEndpoint(collectionId, deleteTarget);
    setDeleteTarget(null);
    await fetchCollection();
  };

  // ── Render ──────────────────────────────────────────────

  if (loading) return <LoadingSpinner />;
  if (!collection) {
    return (
      <EmptyState
        icon={Layers}
        title="Collection Not Found"
        description="The requested collection could not be found."
      />
    );
  }

  const endpoints = collection.endpoints ?? [];

  return (
    <div className={styles.container}>
      <Link to={ROUTES.COLLECTIONS} className={styles.backLink}>
        <ArrowLeft size={14} />
        Back to Collections
      </Link>

      <div className={styles.header}>
        <div className={styles.titleRow}>
          <h1 className={styles.title}>{collection.name}</h1>
          {collection.description && (
            <p className={styles.description}>{collection.description}</p>
          )}
        </div>
        <button className={styles.addBtn} onClick={openAddModal}>
          <Plus size={14} />
          Add Endpoint
        </button>
      </div>

      {endpoints.length === 0 ? (
        <EmptyState
          icon={Layers}
          title="No Endpoints"
          description="Add your first endpoint to this collection."
        />
      ) : (
        <div className={styles.endpointList}>
          {endpoints.map((ep) => (
            <EndpointRow
              key={ep.id}
              endpoint={ep}
              onEdit={openEditModal}
              onDelete={setDeleteTarget}
            />
          ))}
        </div>
      )}

      {/* ── Endpoint Form Modal ──────────────────────────── */}
      {modalOpen && (
        <div className={styles.modalOverlay} onClick={closeModal}>
          <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
            <h2 className={styles.modalTitle}>
              {editEndpoint ? 'Edit Endpoint' : 'Add Endpoint'}
            </h2>

            <div className={styles.formRow}>
              <div className={styles.formGroup}>
                <label className={styles.formLabel} htmlFor="ep-name">
                  Name *
                </label>
                <input
                  id="ep-name"
                  className={styles.formInput}
                  value={form.name}
                  onChange={(e) =>
                    setForm((f) => ({ ...f, name: e.target.value }))
                  }
                  placeholder="e.g. Get User"
                  autoFocus
                />
              </div>
              <div className={styles.formGroup}>
                <label className={styles.formLabel} htmlFor="ep-method">
                  Method
                </label>
                <select
                  id="ep-method"
                  className={styles.formSelect}
                  value={form.method}
                  onChange={(e) =>
                    setForm((f) => ({
                      ...f,
                      method: e.target.value as HttpMethod,
                    }))
                  }
                >
                  {HTTP_METHODS.map((m) => (
                    <option key={m} value={m}>
                      {m}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div className={styles.formGroup}>
              <label className={styles.formLabel} htmlFor="ep-url">
                URL *
              </label>
              <input
                id="ep-url"
                className={styles.formInput}
                value={form.url}
                onChange={(e) =>
                  setForm((f) => ({ ...f, url: e.target.value }))
                }
                placeholder="https://api.example.com/users"
              />
            </div>

            {/* Headers */}
            <div className={styles.headersSection}>
              <label className={styles.formLabel}>Headers</label>
              {form.headers.map((h, i) => (
                <div key={i} className={styles.headerRow}>
                  <input
                    className={styles.headerInput}
                    placeholder="Key"
                    value={h.key}
                    onChange={(e) => updateHeader(i, 'key', e.target.value)}
                  />
                  <input
                    className={styles.headerInput}
                    placeholder="Value"
                    value={h.value}
                    onChange={(e) => updateHeader(i, 'value', e.target.value)}
                  />
                  <button
                    className={styles.removeHeaderBtn}
                    onClick={() => removeHeader(i)}
                    title="Remove header"
                  >
                    <X size={14} />
                  </button>
                </div>
              ))}
              <button className={styles.addHeaderBtn} onClick={addHeader}>
                + Add Header
              </button>
            </div>

            <div className={styles.formGroup}>
              <label className={styles.formLabel} htmlFor="ep-body">
                Body
              </label>
              <textarea
                id="ep-body"
                className={styles.formTextarea}
                value={form.body}
                onChange={(e) =>
                  setForm((f) => ({ ...f, body: e.target.value }))
                }
                placeholder='{"key": "value"}'
              />
            </div>

            <div className={styles.modalActions}>
              <button className={styles.cancelBtn} onClick={closeModal}>
                Cancel
              </button>
              <button
                className={styles.saveBtn}
                onClick={handleSave}
                disabled={!form.name.trim() || !form.url.trim() || saving}
              >
                {saving ? 'Saving…' : editEndpoint ? 'Update' : 'Add'}
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
            <h2 className={styles.modalTitle}>Delete Endpoint?</h2>
            <p className={styles.confirmText}>
              This will permanently remove this endpoint from the collection.
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
