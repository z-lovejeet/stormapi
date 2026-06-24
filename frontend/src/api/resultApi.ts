import { apiClient } from './client';
import type { PaginatedResponse } from '../types/api';
import type { TestResultResponse } from '../types/test';
import type { MetricSnapshotResponse, RequestLogResponse } from '../types/metrics';

/**
 * Result + metrics API module.
 */

export async function getLatestResult(testId: number): Promise<TestResultResponse | null> {
  try {
    const { data } = await apiClient.get<TestResultResponse>(`/tests/${testId}/result`);
    return data;
  } catch {
    return null;
  }
}

export async function getAllResults(testId: number): Promise<TestResultResponse[]> {
  const { data } = await apiClient.get<TestResultResponse[]>(`/tests/${testId}/results`);
  return data;
}

export async function getSnapshots(resultId: number): Promise<MetricSnapshotResponse[]> {
  const { data } = await apiClient.get<MetricSnapshotResponse[]>(
    `/metrics/${resultId}/snapshots`,
  );
  return data;
}

export async function getRequestLogs(
  resultId: number,
  params?: { page?: number; size?: number },
): Promise<PaginatedResponse<RequestLogResponse>> {
  const { data } = await apiClient.get<PaginatedResponse<RequestLogResponse>>(
    `/metrics/${resultId}/request-logs`,
    { params },
  );
  return data;
}
