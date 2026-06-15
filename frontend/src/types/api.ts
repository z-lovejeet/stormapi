/**
 * Generic API response envelope matching backend's ApiResponse<T>.
 * Every API call returns this structure.
 */
export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  error?: ApiError;
  timestamp: string;
  path: string;
}

/**
 * Error details matching backend's ApiResponse.ApiError.
 */
export interface ApiError {
  status: number;
  error: string;
  message: string;
  errorCode: string;
  fieldErrors?: Record<string, string>;
}
