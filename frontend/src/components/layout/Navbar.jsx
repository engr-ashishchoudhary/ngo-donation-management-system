import { Menu } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { humanize } from '../../utils/format';

export default function Navbar({ title, onMenuClick }) {
  const { user } = useAuth();
  const initial = (user?.name || user?.email || '?').trim().charAt(0).toUpperCase();

  return (
    <header className="topbar">
      <div className="topbar-left">
        <button type="button" className="btn btn-ghost btn-icon menu-toggle" onClick={onMenuClick} aria-label="Open menu">
          <Menu size={22} />
        </button>
        <h2 className="topbar-title">{title}</h2>
      </div>
      <div className="user-chip">
        <div className="user-meta">
          <strong>{user?.name}</strong>
          <span>{humanize(user?.role)}</span>
        </div>
        <div className="avatar">{initial}</div>
      </div>
    </header>
  );
}
