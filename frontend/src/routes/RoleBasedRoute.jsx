import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export const homePathForRole = (role) => (role === 'DONOR' ? '/donor/dashboard' : '/admin/dashboard');

// Allows only the listed roles; everyone else is sent to their own home.
export default function RoleBasedRoute({ roles }) {
  const { user } = useAuth();
  if (!user || !roles.includes(user.role)) {
    return <Navigate to={user ? homePathForRole(user.role) : '/'} replace />;
  }
  return <Outlet />;
}
