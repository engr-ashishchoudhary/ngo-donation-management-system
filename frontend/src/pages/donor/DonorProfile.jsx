import { useEffect, useState } from 'react';
import { Pencil, Save } from 'lucide-react';
import PageHeader from '../../components/common/PageHeader';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import ErrorMessage from '../../components/common/ErrorMessage';
import Alert from '../../components/common/Alert';
import FormInput from '../../components/forms/FormInput';
import FormTextarea from '../../components/forms/FormTextarea';
import useAsync from '../../hooks/useAsync';
import { getProfile, updateProfile } from '../../api/donorApi';
import { formatDate, formatDateTime } from '../../utils/format';
import { getErrorMessage } from '../../utils/errors';
import { useToast } from '../../context/ToastContext';

const PHONE_RE = /^[0-9+\-\s()]{7,15}$/;

export default function DonorProfile() {
  const toast = useToast();
  const { data: profile, loading, error, reload, setData } = useAsync(() => getProfile(), []);
  const [editing, setEditing] = useState(false);
  const [values, setValues] = useState({ name: '', phone: '', address: '' });
  const [errors, setErrors] = useState({});
  const [apiError, setApiError] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (profile) setValues({ name: profile.name || '', phone: profile.phone || '', address: profile.address || '' });
  }, [profile]);

  const onChange = (e) => {
    setValues((v) => ({ ...v, [e.target.name]: e.target.value }));
    setErrors((er) => ({ ...er, [e.target.name]: undefined }));
  };

  // Mirrors DonorProfileUpdateRequest: name required ≤100, phone ≤15.
  const validate = () => {
    const next = {};
    if (!values.name.trim()) next.name = 'Name is required';
    else if (values.name.trim().length > 100) next.name = 'Name must not exceed 100 characters';
    if (values.phone.trim() && !PHONE_RE.test(values.phone.trim())) next.phone = 'Enter a valid phone number (max 15 characters)';
    setErrors(next);
    return Object.keys(next).length === 0;
  };

  const cancel = () => {
    setEditing(false);
    setErrors({});
    setApiError('');
    setValues({ name: profile.name || '', phone: profile.phone || '', address: profile.address || '' });
  };

  const onSubmit = async (e) => {
    e.preventDefault();
    setApiError('');
    if (!validate()) return;
    setSaving(true);
    try {
      const updated = await updateProfile({
        name: values.name.trim(),
        phone: values.phone.trim() || null,
        address: values.address.trim() || null,
      });
      setData(updated);
      setEditing(false);
      toast.success('Profile updated.');
    } catch (err) {
      setApiError(getErrorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <LoadingSpinner label="Loading profile…" />;
  if (error) return <ErrorMessage message={error} onRetry={reload} />;

  const initial = (profile.name || '?').trim().charAt(0).toUpperCase();

  return (
    <>
      <PageHeader
        title="My Profile"
        subtitle="Your personal details."
        actions={
          !editing && (
            <button type="button" className="btn btn-primary" onClick={() => setEditing(true)}>
              <Pencil size={16} /> Edit profile
            </button>
          )
        }
      />

      <div className="card" style={{ maxWidth: 720 }}>
        <div className="profile-head">
          <div className="avatar avatar-lg">{initial}</div>
          <div>
            <h2 style={{ fontSize: 18 }}>{profile.name}</h2>
            <div className="cell-muted">Member since {formatDate(profile.createdAt)}</div>
          </div>
        </div>

        {editing ? (
          <form onSubmit={onSubmit} noValidate className="card-pad stack" style={{ gap: 16 }}>
            <Alert type="error">{apiError}</Alert>
            <div className="form-grid">
              <FormInput className="full" label="Full name" name="name" value={values.name} onChange={onChange} error={errors.name} required />
              <FormInput label="Email" value={profile.email || ''} disabled hint="Email cannot be changed" readOnly />
              <FormInput label="Phone number" name="phone" type="tel" value={values.phone} onChange={onChange} error={errors.phone} maxLength={15} />
              <FormTextarea className="full" label="Address" name="address" value={values.address} onChange={onChange} />
            </div>
            <div className="row" style={{ justifyContent: 'flex-end' }}>
              <button type="button" className="btn btn-secondary" onClick={cancel} disabled={saving}>
                Cancel
              </button>
              <button type="submit" className="btn btn-primary" disabled={saving}>
                {saving ? <LoadingSpinner small /> : <Save size={16} />}
                Save changes
              </button>
            </div>
          </form>
        ) : (
          <dl className="detail-grid card-pad">
            <div>
              <dt>Full name</dt>
              <dd>{profile.name}</dd>
            </div>
            <div>
              <dt>Email</dt>
              <dd>{profile.email}</dd>
            </div>
            <div>
              <dt>Phone</dt>
              <dd>{profile.phone || '—'}</dd>
            </div>
            <div>
              <dt>Last updated</dt>
              <dd>{formatDateTime(profile.updatedAt)}</dd>
            </div>
            <div className="full">
              <dt>Address</dt>
              <dd style={{ fontWeight: 400 }}>{profile.address || '—'}</dd>
            </div>
          </dl>
        )}
      </div>
    </>
  );
}
