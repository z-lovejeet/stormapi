import { describe, it, expect } from 'vitest';
import { apiClient } from '../../api/client';

describe('apiClient', () => {
  it('has correct base URL', () => {
    expect(apiClient.defaults.baseURL).toBe('/api');
  });

  it('has correct timeout', () => {
    expect(apiClient.defaults.timeout).toBe(30000);
  });

  it('has JSON content type', () => {
    expect(apiClient.defaults.headers['Content-Type']).toBe('application/json');
  });

  it('has response interceptors configured', () => {
    // Axios stores interceptors in handlers array
    expect((apiClient.interceptors.response as any).handlers.length).toBeGreaterThan(0);
  });
});
