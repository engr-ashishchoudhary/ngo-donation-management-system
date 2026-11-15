import PageHeader from '../../components/common/PageHeader';
import Alert from '../../components/common/Alert';
import StatusBadge from '../../components/common/StatusBadge';
import { useAuth } from '../../context/AuthContext';

// The backend has no admin profile endpoint, so this shows the details returned at login (read-only).
export default function AdminProfile() {
  const { user } = useAuth();
  const initial = (user?.name || '?').trim().charAt(0).toUpperCase();

  return (
    <>
      <PageHeader title="Profile" subtitle="Your administrator account." />
      <div className="card" style={{ maxWidth: 680 }}>
        <div className="profile-head">
          <div className="avatar avatar-lg">{initial}</div>
          <div>
            <h2 style={{ fontSize: 18 }}>{user?.name}</h2>
            <StatusBadge status={user?.role} />
          </div>
        </div>
        <div className="card-pad stack">
          <dl className="detail-grid">
            <div>
              <dt>Name</dt>
              <dd>{user?.name || '—'}</dd>
            </div>
            <div>
              <dt>Email</dt>
              <dd>{user?.email || '—'}</dd>
            </div>
            <div>
              <dt>Role</dt>
              <dd>{user?.role === 'SUPER_ADMIN' ? 'Super Admin' : 'Admin'}</dd>
            </div>
            <div>
              <dt>Administrator ID</dt>
              <dd>#{user?.id}</dd>
            </div>
          </dl>
          <Alert type="info">Profile editing is not available — the backend does not provide an admin profile endpoint.</Alert>
        </div>
      </div>
    </>
  );
}
