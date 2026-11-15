import { LayoutDashboard, Megaphone, HandCoins, UserCircle } from 'lucide-react';
import DashboardLayout from '../components/layout/DashboardLayout';

const ITEMS = [
  { to: '/donor/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/donor/campaigns', label: 'Browse Campaigns', icon: Megaphone },
  { to: '/donor/donations', label: 'My Donations', icon: HandCoins },
  { to: '/donor/profile', label: 'My Profile', icon: UserCircle },
];

export default function DonorLayout() {
  return <DashboardLayout navItems={ITEMS} roleLabel="Donor Portal" loginPath="/donor/login" />;
}
