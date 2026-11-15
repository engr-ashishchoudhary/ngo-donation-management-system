import { STORAGE_KEYS } from './constants';

export const getToken = () => {
  try {
    return localStorage.getItem(STORAGE_KEYS.TOKEN);
  } catch {
    return null;
  }
};

export const getStoredUser = () => {
  try {
    const raw = localStorage.getItem(STORAGE_KEYS.USER);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
};

export const saveAuth = (token, user) => {
  localStorage.setItem(STORAGE_KEYS.TOKEN, token);
  localStorage.setItem(STORAGE_KEYS.USER, JSON.stringify(user));
};

export const clearAuth = () => {
  try {
    localStorage.removeItem(STORAGE_KEYS.TOKEN);
    localStorage.removeItem(STORAGE_KEYS.USER);
  } catch {
    /* storage unavailable */
  }
};

// Read the `exp` claim (seconds) without verifying the signature — the backend
// remains the source of truth; this only avoids sending an obviously dead token.
export const isTokenExpired = (token) => {
  if (!token) return true;
  try {
    const payload = token.split('.')[1];
    const json = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')));
    if (!json.exp) return false;
    return json.exp * 1000 <= Date.now();
  } catch {
    return true;
  }
};
