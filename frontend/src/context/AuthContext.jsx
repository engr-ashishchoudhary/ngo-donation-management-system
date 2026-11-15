import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import * as authApi from '../api/authApi';
import { AUTH_EXPIRED_EVENT } from '../api/axiosInstance';
import { clearAuth, getStoredUser, getToken, isTokenExpired, saveAuth } from '../utils/authStorage';
import { ADMIN_ROLES, ROLES } from '../utils/constants';

const AuthContext = createContext(null);

// The backend returns "SUPER_ADMIN"/"ADMIN" for admins and "DONOR" for donors,
// but tolerate a "ROLE_" prefix as well.
const normalizeRole = (role) => (role ? String(role).replace(/^ROLE_/, '').toUpperCase() : null);

const buildUser = (response) => ({
  id: response.id,
  name: response.name,
  email: response.email,
  role: normalizeRole(response.role),
});

const restoreSession = () => {
  const token = getToken();
  const user = getStoredUser();
  if (!token || !user || isTokenExpired(token)) {
    clearAuth();
    return { token: null, user: null };
  }
  return { token, user };
};

export function AuthProvider({ children }) {
  const [session, setSession] = useState(restoreSession);
  const [sessionExpired, setSessionExpired] = useState(false);

  const login = useCallback(async (kind, credentials) => {
    const loginFn = kind === 'admin' ? authApi.adminLogin : authApi.donorLogin;
    const response = await loginFn(credentials);
    const user = buildUser(response);

    // Make sure the role matches the portal being used.
    const isAdminRole = ADMIN_ROLES.includes(user.role);
    if ((kind === 'admin' && !isAdminRole) || (kind === 'donor' && user.role !== ROLES.DONOR)) {
      throw new Error('This account cannot sign in through this portal.');
    }

    saveAuth(response.token, user);
    setSession({ token: response.token, user });
    setSessionExpired(false);
    return user;
  }, []);

  const logout = useCallback(() => {
    clearAuth();
    setSession({ token: null, user: null });
  }, []);

  // Fired by the Axios layer on 401 / expired token.
  useEffect(() => {
    const handler = () => {
      setSession({ token: null, user: null });
      setSessionExpired(true);
    };
    window.addEventListener(AUTH_EXPIRED_EVENT, handler);
    return () => window.removeEventListener(AUTH_EXPIRED_EVENT, handler);
  }, []);

  // Keep multiple tabs in sync (logout in one tab logs out the others).
  useEffect(() => {
    const onStorage = (e) => {
      if (e.key === null || e.key.startsWith('ngo.auth.')) setSession(restoreSession());
    };
    window.addEventListener('storage', onStorage);
    return () => window.removeEventListener('storage', onStorage);
  }, []);

  const value = useMemo(
    () => ({
      user: session.user,
      token: session.token,
      isAuthenticated: Boolean(session.token && session.user),
      isAdmin: ADMIN_ROLES.includes(session.user?.role),
      isSuperAdmin: session.user?.role === ROLES.SUPER_ADMIN,
      isDonor: session.user?.role === ROLES.DONOR,
      sessionExpired,
      clearSessionExpired: () => setSessionExpired(false),
      login,
      logout,
    }),
    [session, sessionExpired, login, logout]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

// eslint-disable-next-line react-refresh/only-export-components
export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
};
