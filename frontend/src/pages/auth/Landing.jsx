import { Link, Navigate } from 'react-router-dom';
import { HeartHandshake, ShieldCheck, Heart } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { homePathForRole } from '../../routes/RoleBasedRoute';

// Neutral portal chooser — contains no business data.
export default function Landing() {
  const { isAuthenticated, user } = useAuth();
  if (isAuthenticated) return <Navigate to={homePathForRole(user.role)} replace />;

  return (
    <div className="landing">
      <div className="landing-inner">
        <span className="brand-mark" style={{ width: 44, height: 44 }}>
          <HeartHandshake size={24} />
        </span>
        <h1>NGO Donation Management System</h1>
        <p className="sub">Choose how you would like to sign in.</p>
        <div className="grid grid-2">
          <div className="portal-card">
            <Heart size={28} color="#4f46e5" />
            <h3>I am a donor</h3>
            <p>Browse active campaigns, make donations and view your giving history.</p>
            <Link to="/donor/login" className="btn btn-primary">
              Donor sign in
            </Link>
            <Link to="/donor/register" className="btn btn-secondary">
              Create donor account
            </Link>
          </div>
          <div className="portal-card">
            <ShieldCheck size={28} color="#4f46e5" />
            <h3>I am NGO staff</h3>
            <p>Manage campaigns, confirm donations and record expenses.</p>
            <Link to="/admin/login" className="btn btn-primary">
              Admin sign in
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
