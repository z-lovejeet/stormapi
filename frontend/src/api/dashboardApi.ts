import { apiClient } from './client';
import type { DashboardStats } from '../types/dashboard';

/**
 * Dashboard API module.
 */
export async function getStats(): Promise<DashboardStats> {
  const { data } = await apiClient.get<DashboardStats>('/dashboard/stats');
  return data;
}
