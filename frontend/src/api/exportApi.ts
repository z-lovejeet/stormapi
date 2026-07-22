import axios from 'axios';
import { API_BASE } from '../utils/constants';

/**
 * Export API module — blob downloads for JSON, CSV, HTML, and PDF.
 * Uses a dedicated axios instance (no response-envelope interceptor)
 * so binary blob responses are not corrupted by JSON unwrapping.
 */

const exportClient = axios.create({
  baseURL: API_BASE,
  timeout: 60000,
  withCredentials: true,
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN',
});

function triggerDownload(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}

export async function downloadJson(resultId: number, filename?: string): Promise<void> {
  const { data } = await exportClient.get(`/export/${resultId}/json`, {
    responseType: 'blob',
  });
  triggerDownload(data as Blob, filename || `stormapi-result-${resultId}.json`);
}

export async function downloadCsv(resultId: number, filename?: string): Promise<void> {
  const { data } = await exportClient.get(`/export/${resultId}/csv`, {
    responseType: 'blob',
  });
  triggerDownload(data as Blob, filename || `stormapi-metrics-${resultId}.csv`);
}

export async function downloadHtml(resultId: number, filename?: string): Promise<void> {
  const { data } = await exportClient.get(`/export/${resultId}/html`, {
    responseType: 'blob',
  });
  triggerDownload(data as Blob, filename || `stormapi-report-${resultId}.html`);
}
