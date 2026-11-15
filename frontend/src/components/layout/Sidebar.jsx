import { NavLink } from 'react-router-dom';
import { LogOut, HeartHandshake } from 'lucide-react';

export default function Sidebar({ items, roleLabel, open, onNavigate, onLogout }) {
  return (
    <>
      <aside className={`sidebar${open ? ' open' : ''}`} aria-label="Primary navigation">
        <div className="sidebar-brand">
          <span className="brand-mark">
            <HeartHandshake size={20} />
          </span>
          <span>NGO Donations</span>
        </div>
        <div className="sidebar-role">{roleLabel}</div>
        <nav className="sidebar-nav">
          {items.map(({ to, label, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              onClick={onNavigate}
              className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}
            >
              <Icon size={18} />
              {label}
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-footer">
          <button type="button" className="nav-link" onClick={onLogout}>
            <LogOut size={18} />
            Logout
          </button>
        </div>
      </aside>
      <div className={`sidebar-backdrop${open ? ' open' : ''}`} onClick={onNavigate} />
    </>
  );
}
