import axios from 'axios';
import { clearAuth, getToken, isTokenExpired } from '../utils/authStorage';

export const AUTH_EXPIRED_EVENT = 'ngo:auth-expired';

const axiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  headers: { 'Content-Type': 'application/json' },
  timeout: 20000,
});

axiosInstance.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    if (isTokenExpired(token)) {
      clearAuth();
      window.dispatchEvent(new CustomEvent(AUTH_EXPIRED_EVENT));
    } else {
      config.headers.Authorization = `Bearer ${token}`;
    }
  }
  return config;
});

axiosInstance.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    const url = error.config?.url || '';
    const isLoginCall = url.includes('/api/auth/') && url.includes('/login');

    // A 401 on any call other than a login attempt means the session is dead.
    if (status === 401 && !isLoginCall && getToken()) {
      clearAuth();
      window.dispatchEvent(new CustomEvent(AUTH_EXPIRED_EVENT));
    }
    return Promise.reject(error);
  }
);

export default axiosInstance;
