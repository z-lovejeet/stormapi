import { HttpMethod } from './test';

/**
 * API collection matching backend's ApiCollection entity.
 */
export interface ApiCollection {
  id: number;
  name: string;
  description?: string;
  endpoints: ApiEndpoint[];
  createdAt: string;
  updatedAt: string;
}

/**
 * Saved API endpoint matching backend's ApiEndpoint entity.
 */
export interface ApiEndpoint {
  id: number;
  collectionId: number;
  name: string;
  url: string;
  method: HttpMethod;
  headers: KeyValuePair[];
  body?: string;
  description?: string;
  sortOrder: number;
  createdAt: string;
  updatedAt: string;
}

/**
 * Key-value pair matching backend's KeyValuePair @Embeddable.
 */
export interface KeyValuePair {
  key: string;
  value: string;
}
