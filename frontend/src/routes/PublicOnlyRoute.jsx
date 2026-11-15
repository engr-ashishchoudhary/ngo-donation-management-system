import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { homePathForRole } from './RoleBasedRoute';

// Login/registration pages: signed-in users go straight to their dashboard.
export default function PublicOnlyRoute({ children }) {
  const { isAuthenticated, user } = useAuth();
  if (isAuthenticated) return <Navigate to={homePathForRole(user.role)} replace />;
  return children;
}
