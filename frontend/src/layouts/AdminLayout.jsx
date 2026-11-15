import {
  LayoutDashboard,
  Megaphone,
  HandCoins,
  Receipt,
  Users,
  UserCircle,
  ShieldCheck,
} from 'lucide-react';
import DashboardLayout from '../components/layout/DashboardLayout';
import { useAuth } from '../context/AuthContext';
import { humanize } from '../utils/format';

const BASE_ITEMS = [
  { to: '/admin/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/admin/campaigns', label: 'Campaigns', icon: Megaphone },
  { to: '/admin/donations', label: 'Donations', icon: HandCoins },
  { to: '/admin/expenses', label: 'Expenses', icon: Receipt },
  { to: '/admin/donors', label: 'Donors', icon: Users },
  { to: '/admin/profile', label: 'Profile', icon: UserCircle },
];

const SUPER_ADMIN_ITEMS = [{ to: '/admin/admins', label: 'Admin Management', icon: ShieldCheck }];

export default function AdminLayout() {
  const { isSuperAdmin, user } = useAuth();
  const items = isSuperAdmin ? [...BASE_ITEMS, ...SUPER_ADMIN_ITEMS] : BASE_ITEMS;
  return <DashboardLayout navItems={items} roleLabel={`${humanize(user?.role)} Panel`} loginPath="/admin/login" />;
}
