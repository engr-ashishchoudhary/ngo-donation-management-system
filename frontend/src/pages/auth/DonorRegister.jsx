import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { UserPlus } from 'lucide-react';
import AuthLayout from '../../layouts/AuthLayout';
import FormInput from '../../components/forms/FormInput';
import FormTextarea from '../../components/forms/FormTextarea';
import Alert from '../../components/common/Alert';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import { donorRegister } from '../../api/authApi';
import { getErrorMessage } from '../../utils/errors';
import { useToast } from '../../context/ToastContext';

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const PHONE_RE = /^[0-9+\-\s()]{7,15}$/;

const initial = { name: '', email: '', phone: '', address: '', password: '', confirmPassword: '' };

export default function DonorRegister() {
  const navigate = useNavigate();
  const toast = useToast();
  const [values, setValues] = useState(initial);
  const [errors, setErrors] = useState({});
  const [apiError, setApiError] = useState('');
  const [loading, setLoading] = useState(false);

  const onChange = (e) => {
    setValues((v) => ({ ...v, [e.target.name]: e.target.value }));
    setErrors((er) => ({ ...er, [e.target.name]: undefined }));
  };

  // Mirrors DonorRegisterRequest: name ≤100, email ≤150, password 6–100, phone ≤15.
  const validate = () => {
    const next = {};
    if (!values.name.trim()) next.name = 'Full name is required';
    else if (values.name.trim().length > 100) next.name = 'Name must not exceed 100 characters';

    if (!values.email.trim()) next.email = 'Email is required';
    else if (!EMAIL_RE.test(values.email.trim())) next.email = 'Enter a valid email address';
    else if (values.email.trim().length > 150) next.email = 'Email must not exceed 150 characters';

    if (values.phone.trim() && !PHONE_RE.test(values.phone.trim()))
      next.phone = 'Enter a valid phone number (max 15 characters)';

    if (!values.password) next.password = 'Password is required';
    else if (values.password.length < 6 || values.password.length > 100)
      next.password = 'Password must be between 6 and 100 characters';

    if (!values.confirmPassword) next.confirmPassword = 'Please confirm your password';
    else if (values.confirmPassword !== values.password) next.confirmPassword = 'Passwords do not match';

    setErrors(next);
    return Object.keys(next).length === 0;
  };

  const onSubmit = async (e) => {
    e.preventDefault();
    setApiError('');
    if (!validate()) return;
    setLoading(true);
    try {
      await donorRegister({
        name: values.name.trim(),
        email: values.email.trim(),
        password: values.password,
        phone: values.phone.trim() || null,
        address: values.address.trim() || null,
      });
      toast.success('Registration successful. Please sign in.');
      navigate('/donor/login', { replace: true });
    } catch (err) {
      setApiError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthLayout
      heading="Join our community of donors."
      blurb="Create a free account to donate to active campaigns and track every contribution you make."
    >
      <div className="card auth-card wide">
        <h1>Create donor account</h1>
        <p className="lead">It only takes a minute.</p>
        <form onSubmit={onSubmit} noValidate className="stack" style={{ gap: 16 }}>
          <Alert type="error">{apiError}</Alert>
          <div className="form-grid">
            <FormInput
              className="full"
              label="Full name"
              name="name"
              value={values.name}
              onChange={onChange}
              error={errors.name}
              autoComplete="name"
              required
            />
            <FormInput
              label="Email"
              name="email"
              type="email"
              value={values.email}
              onChange={onChange}
              error={errors.email}
              autoComplete="email"
              required
            />
            <FormInput
              label="Phone number"
              name="phone"
              type="tel"
              value={values.phone}
              onChange={onChange}
              error={errors.phone}
              autoComplete="tel"
              maxLength={15}
            />
            <FormTextarea
              className="full"
              label="Address"
              name="address"
              value={values.address}
              onChange={onChange}
              error={errors.address}
              rows={3}
              autoComplete="street-address"
            />
            <FormInput
              label="Password"
              name="password"
              type="password"
              value={values.password}
              onChange={onChange}
              error={errors.password}
              autoComplete="new-password"
              hint="6–100 characters"
              required
            />
            <FormInput
              label="Confirm password"
              name="confirmPassword"
              type="password"
              value={values.confirmPassword}
              onChange={onChange}
              error={errors.confirmPassword}
              autoComplete="new-password"
              required
            />
          </div>
          <button type="submit" className="btn btn-primary btn-block" disabled={loading}>
            {loading ? <LoadingSpinner small /> : <UserPlus size={16} />}
            {loading ? 'Creating account…' : 'Create account'}
          </button>
        </form>
        <div className="auth-switch">
          Already registered? <Link to="/donor/login">Sign in</Link>
        </div>
      </div>
    </AuthLayout>
  );
}
