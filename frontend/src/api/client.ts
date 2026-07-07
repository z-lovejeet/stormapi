import axios from 'axios';
import type { ApiResponse, ApiError } from '../types/api';

/**
 * Axios instance configured for StormAPI backend.
 * Response interceptor unwraps ApiResponse<T> envelope.
 */
export const apiClient = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' },
  withCredentials: true,
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN',
});

// ── Response Interceptor: unwrap ApiResponse<T> ───────────
apiClient.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResponse<unknown>;
    // If it has the envelope structure, unwrap it
    if (body && typeof body === 'object' && 'success' in body) {
      if (body.success) {
        response.data = body.data;
      } else {
        const apiError = body.error as ApiError;
        return Promise.reject(apiError);
      }
    }
    return response;
  },
  (error) => {
    if (axios.isAxiosError(error) && error.response) {
      const body = error.response.data as ApiResponse<unknown> | undefined;
      if (body?.error) {
        return Promise.reject(body.error);
      }
    }
    return Promise.reject({
      status: 0,
      error: 'Network Error',
      message: error.message || 'Unable to connect to server',
      errorCode: 'NETWORK_ERROR',
    } satisfies ApiError);
  },
);

// ── Request Interceptor: dev logging ──────────────────────
if (import.meta.env.DEV) {
  apiClient.interceptors.request.use((config) => {
    console.debug(`[API] ${config.method?.toUpperCase()} ${config.url}`);
    return config;
  });
}
