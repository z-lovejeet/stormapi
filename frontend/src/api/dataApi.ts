import { apiClient } from './client';
import type { DataDrivenRequest, DataDrivenExecutionResponse } from '../types/data';

/**
 * API functions for data-driven (parameterized) test execution.
 */
export const dataApi = {
  /**
   * Execute a scenario with parameterized data rows.
   */
  execute: async (request: DataDrivenRequest): Promise<DataDrivenExecutionResponse> => {
    const { data } = await apiClient.post<DataDrivenExecutionResponse>(
      '/data-driven/execute',
      request,
    );
    return data;
  },
};
