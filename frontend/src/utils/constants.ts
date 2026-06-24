/** API base path — Vite proxy forwards to Spring Boot */
export const API_BASE = '/api';

/** WebSocket endpoint */
export const WS_ENDPOINT = '/ws';

/** Route paths */
export const ROUTES = {
  DASHBOARD: '/dashboard',
  TEST_BUILDER: '/tests/new',
  LIVE_MONITOR: (id: number | string) => `/tests/${id}/live`,
  TEST_RESULT: (id: number | string) => `/tests/${id}/result`,
  HISTORY: '/history',
  COLLECTIONS: '/collections',
  SETTINGS: '/settings',
} as const;

/** Default test configuration values */
export const TEST_DEFAULTS = {
  virtualUsers: 10,
  durationSeconds: 30,
  rampUpSeconds: 5,
  maxRetries: 0,
  timeoutMs: 5000,
  thinkTimeMs: 0,
} as const;

/** Pagination defaults */
export const PAGINATION = {
  DEFAULT_PAGE: 0,
  DEFAULT_SIZE: 20,
  SIZES: [10, 20, 50, 100] as const,
} as const;

/** Chart sliding window size */
export const CHART_MAX_POINTS = 60;
