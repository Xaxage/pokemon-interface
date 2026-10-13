import { request } from './client';
import type { AccessToken, Credentials, User } from './types';

export const authApi = {
  register: (credentials: Credentials) => request<User>('/auth/register', { method: 'POST', body: credentials }),

  signIn: (credentials: Credentials) => request<AccessToken>('/auth/sign-in', { method: 'POST', body: credentials }),
};
