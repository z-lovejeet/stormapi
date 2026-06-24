import { apiClient } from './client';
import type { PaginatedResponse } from '../types/api';
import type {
  CreateTestRequest,
  TestConfigResponse,
  TestResultResponse,
  TestSummaryResponse,
  TestStatus,
  TestType,
} from '../types/test';

/**
 * Test CRUD + execution API module.
 */

export async function createTest(req: CreateTestRequest): Promise<TestConfigResponse> {
  const { data } = await apiClient.post<TestConfigResponse>('/tests', req);
  return data;
}

export async function listTests(params?: {
  page?: number;
  size?: number;
  sort?: string;
  status?: TestStatus;
  type?: TestType;
}): Promise<PaginatedResponse<TestSummaryResponse>> {
  const { data } = await apiClient.get<PaginatedResponse<TestSummaryResponse>>('/tests', { params });
  return data;
}

export async function getTest(id: number): Promise<TestConfigResponse> {
  const { data } = await apiClient.get<TestConfigResponse>(`/tests/${id}`);
  return data;
}

export async function startTest(id: number): Promise<TestResultResponse> {
  const { data } = await apiClient.post<TestResultResponse>(`/tests/${id}/start`);
  return data;
}

export async function stopTest(id: number): Promise<void> {
  await apiClient.post(`/tests/${id}/stop`);
}

export async function rerunTest(id: number): Promise<TestResultResponse> {
  const { data } = await apiClient.post<TestResultResponse>(`/tests/${id}/rerun`);
  return data;
}

export async function deleteTest(id: number): Promise<void> {
  await apiClient.delete(`/tests/${id}`);
}
