import { apiClient } from './client';
import type { AuthStatus, AuthUser } from '../types/auth';

/**
 * Auth API functions.
 */

/** Check current auth status (public — no auth required) */
export async function getAuthStatus(): Promise<AuthStatus> {
  const response = await apiClient.get<AuthStatus>('/auth/status');
  return response.data;
}

/** Get current user profile (requires auth) */
export async function getCurrentUser(): Promise<AuthUser> {
  const response = await apiClient.get<AuthUser>('/auth/me');
  return response.data;
}

/** Logout — clears JWT cookie */
export async function logout(): Promise<void> {
  await apiClient.post('/auth/logout');
}

export async function loginLocal(data: { email: string; password: string }): Promise<AuthUser> {
  const response = await apiClient.post<AuthUser>('/auth/login', data);
  return response.data;
}

export async function registerLocal(data: { name: string; email: string; password: string }): Promise<AuthUser> {
  const response = await apiClient.post<AuthUser>('/auth/register', data);
  return response.data;
}

/** OAuth2 login redirect URLs */
export const OAUTH_URLS = {
  GOOGLE: '/oauth2/authorization/google',
  GITHUB: '/oauth2/authorization/github',
} as const;
