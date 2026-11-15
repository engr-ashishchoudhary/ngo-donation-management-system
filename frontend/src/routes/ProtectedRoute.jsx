import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

// Requires any authenticated user.
export default function ProtectedRoute() {
  const { isAuthenticated, isDonor } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    // Send the visitor to the portal they were trying to reach.
    const target = location.pathname.startsWith('/donor') ? '/donor/login' : '/admin/login';
    return <Navigate to={target} replace state={{ from: location }} />;
  }
  return <Outlet context={{ isDonor }} />;
}
