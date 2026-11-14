import { api as client } from './client';
import type { MigrationRun, AnalysisReport, ConvertedScript, ValidationResult } from '../types';

// Thin wrapper so the paths below stay relative to /migrations.
const api = {
  get: (url: string, config?: Parameters<typeof client.get>[1]) => client.get(`/migrations${url}`, config),
  post: (url: string, data?: unknown, config?: Parameters<typeof client.post>[2]) =>
    client.post(`/migrations${url}`, data, config),
};

export const migrationApi = {
  upload: async (file: File): Promise<MigrationRun> => {
    const formData = new FormData();
    formData.append('file', file);
    const response = await api.post('/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },

  analyze: async (id: string): Promise<AnalysisReport> => {
    const response = await api.post(`/${id}/analyze`);
    return response.data;
  },

  getRun: async (id: string): Promise<MigrationRun> => {
    const response = await api.get(`/${id}`);
    return response.data;
  },

  getAnalysis: async (id: string): Promise<AnalysisReport> => {
    const response = await api.get(`/${id}/analysis`);
    return response.data;
  },

  convert: async (id: string): Promise<ConvertedScript> => {
    const response = await api.post(`/${id}/convert`);
    return response.data;
  },

  getConvertedScript: async (id: string): Promise<ConvertedScript> => {
    const response = await api.get(`/${id}/converted-script`);
    return response.data;
  },

  validate: async (id: string): Promise<ValidationResult> => {
    const response = await api.post(`/${id}/validate`);
    return response.data;
  },

  getValidation: async (id: string): Promise<ValidationResult> => {
    const response = await api.get(`/${id}/validation`);
    return response.data;
  },

  exportPdf: async (id: string): Promise<Blob> => {
    const response = await api.get(`/${id}/export/pdf`, { responseType: 'blob' });
    return response.data;
  },

  exportJson: async (id: string): Promise<Blob> => {
    const response = await api.get(`/${id}/export/json`, { responseType: 'blob' });
    return response.data;
  },

  getHistory: async (): Promise<MigrationRun[]> => {
    const response = await api.get('/history');
    return response.data;
  },

  connectToDb: async (config: {
    host: string;
    port: number;
    serviceName: string;
    username: string;
    password: string;
    schema?: string;
  }): Promise<MigrationRun> => {
    const response = await api.post('/connect', config);
    return response.data;
  },
};
