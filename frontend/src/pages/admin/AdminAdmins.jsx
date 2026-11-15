import { useMemo, useState } from 'react';
import { UserPlus } from 'lucide-react';
import PageHeader from '../../components/common/PageHeader';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import ErrorMessage from '../../components/common/ErrorMessage';
import Alert from '../../components/common/Alert';
import Modal from '../../components/common/Modal';
import StatusBadge from '../../components/common/StatusBadge';
import SearchBar from '../../components/forms/SearchBar';
import FormInput from '../../components/forms/FormInput';
import FormSelect from '../../components/forms/FormSelect';
import DataTable from '../../components/tables/DataTable';
import useAsync from '../../hooks/useAsync';
import { getAdmins } from '../../api/adminApi';
import { adminRegister } from '../../api/authApi';
import { formatDate, humanize } from '../../utils/format';
import { getErrorMessage } from '../../utils/errors';
import { useToast } from '../../context/ToastContext';
import { useAuth } from '../../context/AuthContext';

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
// Roles supported by the backend's AdminRole enum.
const ROLE_OPTIONS = [
  { value: 'ADMIN', label: 'Admin' },
  { value: 'SUPER_ADMIN', label: 'Super Admin' },
];
const blank = { name: '', email: '', password: '', role: 'ADMIN' };

export default function AdminAdmins() {
  const toast = useToast();
  const { user } = useAuth();
  const [query, setQuery] = useState('');
  const [open, setOpen] = useState(false);
  const [values, setValues] = useState(blank);
  const [errors, setErrors] = useState({});
  const [apiError, setApiError] = useState('');
  const [saving, setSaving] = useState(false);

  const { data, loading, error, reload } = useAsync(() => getAdmins(), [], { initialData: [] });

  const rows = useMemo(() => {
    const q = query.trim().toLowerCase();
    return (data || []).filter((a) => !q || [a.name, a.email].some((v) => v?.toLowerCase().includes(q)));
  }, [data, query]);

  const openForm = () => {
    setValues(blank);
    setErrors({});
    setApiError('');
    setOpen(true);
  };

  const onChange = (e) => {
    setValues((v) => ({ ...v, [e.target.name]: e.target.value }));
    setErrors((er) => ({ ...er, [e.target.name]: undefined }));
  };

  // Mirrors AdminRegisterRequest: name ≤100, email ≤150, password 6–100.
  const validate = () => {
    const next = {};
    if (!values.name.trim()) next.name = 'Name is required';
    else if (values.name.trim().length > 100) next.name = 'Name must not exceed 100 characters';
    if (!values.email.trim()) next.email = 'Email is required';
    else if (!EMAIL_RE.test(values.email.trim())) next.email = 'Enter a valid email address';
    else if (values.email.trim().length > 150) next.email = 'Email must not exceed 150 characters';
    if (!values.password) next.password = 'Password is required';
    else if (values.password.length < 6 || values.password.length > 100) next.password = 'Password must be between 6 and 100 characters';
    setErrors(next);
    return Object.keys(next).length === 0;
  };

  const onSubmit = async (e) => {
    e.preventDefault();
    setApiError('');
    if (!validate()) return;
    setSaving(true);
    try {
      await adminRegister({
        name: values.name.trim(),
        email: values.email.trim(),
        password: values.password,
        role: values.role,
      });
      toast.success('Administrator registered.');
      setOpen(false);
      reload();
    } catch (err) {
      setApiError(getErrorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  const columns = [
    {
      key: 'name',
      header: 'Administrator',
      render: (a) => (
        <>
          <div className="cell-strong">
            {a.name} {a.adminId === user?.id && <span className="badge badge-neutral">You</span>}
          </div>
          <div className="cell-muted">#{a.adminId}</div>
        </>
      ),
    },
    { key: 'email', header: 'Email' },
    { key: 'role', header: 'Role', render: (a) => <StatusBadge status={a.role} /> },
    {
      key: 'isActive',
      header: 'Account',
      render: (a) => <span className={`badge badge-${a.isActive ? 'success' : 'neutral'}`}>{a.isActive ? 'Active' : 'Inactive'}</span>,
    },
    { key: 'createdAt', header: 'Created', render: (a) => <span className="cell-muted">{formatDate(a.createdAt)}</span> },
  ];

  return (
    <>
      <PageHeader
        title="Admin Management"
        subtitle="Super Admin only — view administrators and register new ones."
        actions={
          <button type="button" className="btn btn-primary" onClick={openForm}>
            <UserPlus size={16} /> Register admin
          </button>
        }
      />

      <div className="card">
        <div className="toolbar">
          <SearchBar value={query} onChange={setQuery} placeholder="Search name or email" />
        </div>
        {loading ? (
          <LoadingSpinner />
        ) : error ? (
          <ErrorMessage message={error} onRetry={reload} />
        ) : (
          <DataTable columns={columns} rows={rows} rowKey={(a) => a.adminId} emptyTitle="No administrators found" />
        )}
      </div>

      <Modal
        open={open}
        title="Register administrator"
        onClose={() => setOpen(false)}
        closeDisabled={saving}
        footer={
          <>
            <button type="button" className="btn btn-secondary" onClick={() => setOpen(false)} disabled={saving}>
              Cancel
            </button>
            <button type="submit" form="admin-form" className="btn btn-primary" disabled={saving}>
              {saving && <LoadingSpinner small />}
              Register
            </button>
          </>
        }
      >
        <form id="admin-form" onSubmit={onSubmit} noValidate className="stack" style={{ gap: 16 }}>
          <Alert type="error">{apiError}</Alert>
          <div className="form-grid">
            <FormInput className="full" label="Full name" name="name" value={values.name} onChange={onChange} error={errors.name} required />
            <FormInput className="full" label="Email" name="email" type="email" value={values.email} onChange={onChange} error={errors.email} required />
            <FormInput
              label="Temporary password"
              name="password"
              type="password"
              value={values.password}
              onChange={onChange}
              error={errors.password}
              autoComplete="new-password"
              hint="6–100 characters"
              required
            />
            <FormSelect label="Role" name="role" value={values.role} onChange={onChange} options={ROLE_OPTIONS} hint={`Selected: ${humanize(values.role)}`} />
          </div>
        </form>
      </Modal>
    </>
  );
}
