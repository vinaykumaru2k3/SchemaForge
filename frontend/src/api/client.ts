import axios, { AxiosError } from 'axios';

export const TOKEN_KEY = 'token';

/** Shape of every error body the backend returns (see ApiErrorResponse.java). */
export interface ApiErrorBody {
  timestamp?: string;
  status?: number;
  error?: string;
  message?: string;
  path?: string;
  fieldErrors?: Record<string, string>;
}

export const api = axios.create({
  baseURL: `${import.meta.env.VITE_API_URL || ''}/api/v1`,
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

let unauthorizedHandler: (() => void) | null = null;

/** Registered by AuthProvider so an expired or revoked token logs the user out. */
export function setUnauthorizedHandler(handler: (() => void) | null) {
  unauthorizedHandler = handler;
}

api.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    const isAuthCall = error.config?.url?.startsWith('/auth/');
    if (error.response?.status === 401 && !isAuthCall && unauthorizedHandler) {
      unauthorizedHandler();
    }
    return Promise.reject(error);
  },
);

/** Turns any thrown value into a message suitable for showing to the user. */
export function getApiErrorMessage(err: unknown, fallback: string): string {
  if (axios.isAxiosError(err)) {
    if (!err.response) {
      return 'Could not reach the server. Check that the backend is running.';
    }
    const data = err.response.data as ApiErrorBody | string | undefined;
    if (typeof data === 'string' && data.trim()) {
      return data;
    }
    if (data && typeof data === 'object') {
      if (data.fieldErrors && Object.keys(data.fieldErrors).length > 0) {
        return Object.entries(data.fieldErrors)
          .map(([field, msg]) => `${field}: ${msg}`)
          .join('\n');
      }
      if (data.message) {
        return data.message;
      }
    }
  }
  return fallback;
}
