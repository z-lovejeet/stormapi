import { apiClient } from './client';
import type { ApiCollection, ApiEndpoint } from '../types/collection';

/**
 * Collection CRUD API module.
 */

// ── Collection-level ──────────────────────────────────────

export async function createCollection(req: {
  name: string;
  description?: string;
}): Promise<ApiCollection> {
  const { data } = await apiClient.post<ApiCollection>('/collections', req);
  return data;
}

export async function listCollections(): Promise<ApiCollection[]> {
  const { data } = await apiClient.get<ApiCollection[]>('/collections');
  return data;
}

export async function getCollection(id: number): Promise<ApiCollection> {
  const { data } = await apiClient.get<ApiCollection>(`/collections/${id}`);
  return data;
}

export async function updateCollection(
  id: number,
  req: { name: string; description?: string },
): Promise<ApiCollection> {
  const { data } = await apiClient.put<ApiCollection>(`/collections/${id}`, req);
  return data;
}

export async function deleteCollection(id: number): Promise<void> {
  await apiClient.delete(`/collections/${id}`);
}

// ── Endpoint-level ────────────────────────────────────────

export async function addEndpoint(
  collectionId: number,
  req: Omit<ApiEndpoint, 'id' | 'collectionId' | 'createdAt' | 'updatedAt'>,
): Promise<ApiEndpoint> {
  const { data } = await apiClient.post<ApiEndpoint>(
    `/collections/${collectionId}/endpoints`,
    req,
  );
  return data;
}

export async function updateEndpoint(
  collectionId: number,
  endpointId: number,
  req: Omit<ApiEndpoint, 'id' | 'collectionId' | 'createdAt' | 'updatedAt'>,
): Promise<ApiEndpoint> {
  const { data } = await apiClient.put<ApiEndpoint>(
    `/collections/${collectionId}/endpoints/${endpointId}`,
    req,
  );
  return data;
}

export async function deleteEndpoint(
  collectionId: number,
  endpointId: number,
): Promise<void> {
  await apiClient.delete(`/collections/${collectionId}/endpoints/${endpointId}`);
}
