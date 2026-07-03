import { apiClient } from './client';

/**
 * Export API module — blob downloads for JSON, CSV, HTML, and PDF.
 */

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
  const { data } = await apiClient.get(`/export/${resultId}/json`, {
    responseType: 'blob',
  });
  triggerDownload(data as Blob, filename || `result-${resultId}.json`);
}

export async function downloadCsv(resultId: number, filename?: string): Promise<void> {
  const { data } = await apiClient.get(`/export/${resultId}/csv`, {
    responseType: 'blob',
  });
  triggerDownload(data as Blob, filename || `result-${resultId}.csv`);
}

export async function downloadHtml(resultId: number, filename?: string): Promise<void> {
  const { data } = await apiClient.get(`/export/${resultId}/html`, {
    responseType: 'blob',
  });
  triggerDownload(data as Blob, filename || `stormapi-report-${resultId}.html`);
}

export async function downloadPdf(resultId: number, filename?: string): Promise<void> {
  const { data } = await apiClient.get(`/export/${resultId}/pdf`, {
    responseType: 'blob',
  });
  triggerDownload(data as Blob, filename || `stormapi-report-${resultId}.pdf`);
}
