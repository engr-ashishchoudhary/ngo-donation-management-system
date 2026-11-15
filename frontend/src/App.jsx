import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ToastProvider } from './context/ToastContext';
import ProtectedRoute from './routes/ProtectedRoute';
import RoleBasedRoute from './routes/RoleBasedRoute';
import PublicOnlyRoute from './routes/PublicOnlyRoute';
import AdminLayout from './layouts/AdminLayout';
import DonorLayout from './layouts/DonorLayout';
import Landing from './pages/auth/Landing';
import AdminLogin from './pages/auth/AdminLogin';
import DonorLogin from './pages/auth/DonorLogin';
import DonorRegister from './pages/auth/DonorRegister';
import NotFound from './pages/NotFound';
import AdminDashboard from './pages/admin/AdminDashboard';
import AdminCampaigns from './pages/admin/AdminCampaigns';
import AdminDonations from './pages/admin/AdminDonations';
import AdminExpenses from './pages/admin/AdminExpenses';
import AdminDonors from './pages/admin/AdminDonors';
import AdminAdmins from './pages/admin/AdminAdmins';
import AdminProfile from './pages/admin/AdminProfile';
import DonorDashboard from './pages/donor/DonorDashboard';
import DonorCampaigns from './pages/donor/DonorCampaigns';
import DonorDonations from './pages/donor/DonorDonations';
import DonorProfile from './pages/donor/DonorProfile';
import { ADMIN_ROLES, ROLES } from './utils/constants';

export default function App() {
  return (
    <BrowserRouter>
      <ToastProvider>
        <AuthProvider>
          <Routes>
            <Route path="/" element={<Landing />} />

            {/* Public auth pages (signed-in users are redirected away) */}
            <Route path="/admin/login" element={<PublicOnlyRoute><AdminLogin /></PublicOnlyRoute>} />
            <Route path="/donor/login" element={<PublicOnlyRoute><DonorLogin /></PublicOnlyRoute>} />
            <Route path="/donor/register" element={<PublicOnlyRoute><DonorRegister /></PublicOnlyRoute>} />

            {/* Authenticated area */}
            <Route element={<ProtectedRoute />}>
              {/* Admin panel: ADMIN + SUPER_ADMIN */}
              <Route element={<RoleBasedRoute roles={ADMIN_ROLES} />}>
                <Route path="/admin" element={<AdminLayout />}>
                  <Route index element={<Navigate to="dashboard" replace />} />
                  <Route path="dashboard" element={<AdminDashboard />} />
                  <Route path="campaigns" element={<AdminCampaigns />} />
                  <Route path="donations" element={<AdminDonations />} />
                  <Route path="expenses" element={<AdminExpenses />} />
                  <Route path="donors" element={<AdminDonors />} />
                  <Route path="profile" element={<AdminProfile />} />
                  {/* SUPER_ADMIN only */}
                  <Route element={<RoleBasedRoute roles={[ROLES.SUPER_ADMIN]} />}>
                    <Route path="admins" element={<AdminAdmins />} />
                  </Route>
                </Route>
              </Route>

              {/* Donor panel */}
              <Route element={<RoleBasedRoute roles={[ROLES.DONOR]} />}>
                <Route path="/donor" element={<DonorLayout />}>
                  <Route index element={<Navigate to="dashboard" replace />} />
                  <Route path="dashboard" element={<DonorDashboard />} />
                  <Route path="campaigns" element={<DonorCampaigns />} />
                  <Route path="donations" element={<DonorDonations />} />
                  <Route path="profile" element={<DonorProfile />} />
                </Route>
              </Route>
            </Route>

            <Route path="*" element={<NotFound />} />
          </Routes>
        </AuthProvider>
      </ToastProvider>
    </BrowserRouter>
  );
}
