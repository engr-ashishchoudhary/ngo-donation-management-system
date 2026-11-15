import { useEffect, useState } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import Sidebar from './Sidebar';
import Navbar from './Navbar';
import { useAuth } from '../../context/AuthContext';

// Shared shell used by both the admin and donor experiences.
export default function DashboardLayout({ navItems, roleLabel, loginPath }) {
  const [menuOpen, setMenuOpen] = useState(false);
  const { logout } = useAuth();
  const navigate = useNavigate();
  const { pathname } = useLocation();

  useEffect(() => {
    setMenuOpen(false);
  }, [pathname]);

  const current = navItems.find((i) => pathname === i.to || pathname.startsWith(`${i.to}/`));

  const handleLogout = () => {
    logout();
    navigate(loginPath, { replace: true });
  };

  return (
    <div className="app-shell">
      <Sidebar
        items={navItems}
        roleLabel={roleLabel}
        open={menuOpen}
        onNavigate={() => setMenuOpen(false)}
        onLogout={handleLogout}
      />
      <div className="main">
        <Navbar title={current?.label || 'Dashboard'} onMenuClick={() => setMenuOpen(true)} />
        <main className="content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
