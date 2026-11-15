import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { LogIn } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import FormInput from '../../components/forms/FormInput';
import Alert from '../../components/common/Alert';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import { getErrorMessage } from '../../utils/errors';
import { homePathForRole } from '../../routes/RoleBasedRoute';

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

// Shared by the admin and donor login pages; `kind` selects the API.
export default function LoginForm({ kind, title, lead, footer }) {
  const { login, sessionExpired, clearSessionExpired } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [values, setValues] = useState({ email: '', password: '' });
  const [errors, setErrors] = useState({});
  const [apiError, setApiError] = useState('');
  const [loading, setLoading] = useState(false);

  const onChange = (e) => {
    setValues((v) => ({ ...v, [e.target.name]: e.target.value }));
    setErrors((er) => ({ ...er, [e.target.name]: undefined }));
  };

  const validate = () => {
    const next = {};
    if (!values.email.trim()) next.email = 'Email is required';
    else if (!EMAIL_RE.test(values.email.trim())) next.email = 'Enter a valid email address';
    if (!values.password) next.password = 'Password is required';
    setErrors(next);
    return Object.keys(next).length === 0;
  };

  const onSubmit = async (e) => {
    e.preventDefault();
    setApiError('');
    clearSessionExpired();
    if (!validate()) return;
    setLoading(true);
    try {
      const user = await login(kind, { email: values.email.trim(), password: values.password });
      const from = location.state?.from?.pathname;
      const allowedPrefix = kind === 'admin' ? '/admin' : '/donor';
      navigate(from && from.startsWith(allowedPrefix) ? from : homePathForRole(user.role), { replace: true });
    } catch (err) {
      setApiError(err.response ? getErrorMessage(err) : err.message || getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="card auth-card">
      <h1>{title}</h1>
      <p className="lead">{lead}</p>
      <form onSubmit={onSubmit} noValidate className="stack" style={{ gap: 16 }}>
        {sessionExpired && <Alert type="warning">Your session expired. Please sign in again.</Alert>}
        <Alert type="error">{apiError}</Alert>
        <FormInput
          label="Email"
          name="email"
          type="email"
          autoComplete="username"
          value={values.email}
          onChange={onChange}
          error={errors.email}
          placeholder="you@example.org"
          required
        />
        <FormInput
          label="Password"
          name="password"
          type="password"
          autoComplete="current-password"
          value={values.password}
          onChange={onChange}
          error={errors.password}
          required
        />
        <button type="submit" className="btn btn-primary btn-block" disabled={loading}>
          {loading ? <LoadingSpinner small /> : <LogIn size={16} />}
          {loading ? 'Signing in…' : 'Sign in'}
        </button>
      </form>
      <div className="auth-switch">{footer}</div>
      <div className="auth-switch" style={{ marginTop: 8 }}>
        <Link to="/">← Back to portal selection</Link>
      </div>
    </div>
  );
}
