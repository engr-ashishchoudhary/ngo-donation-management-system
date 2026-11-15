import { Link } from 'react-router-dom';
import { Compass } from 'lucide-react';
import EmptyState from '../components/common/EmptyState';
import { useAuth } from '../context/AuthContext';
import { homePathForRole } from '../routes/RoleBasedRoute';

export default function NotFound() {
  const { isAuthenticated, user } = useAuth();
  return (
    <div style={{ minHeight: '100%', display: 'grid', placeItems: 'center' }}>
      <EmptyState
        icon={Compass}
        title="Page not found"
        message="The page you are looking for does not exist."
        action={
          <Link className="btn btn-primary" to={isAuthenticated ? homePathForRole(user.role) : '/'}>
            Go to {isAuthenticated ? 'dashboard' : 'home'}
          </Link>
        }
      />
    </div>
  );
}
