import { api } from './client';

export const authApi = {
  login: async (username: string, password: string): Promise<{ token: string }> => {
    const response = await api.post('/auth/login', { username, password });
    return response.data;
  },
  register: async (username: string, password: string): Promise<{ token: string }> => {
    const response = await api.post('/auth/register', { username, password });
    return response.data;
  },
};
