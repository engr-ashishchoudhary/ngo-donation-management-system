import { Link } from 'react-router-dom';
import AuthLayout from '../../layouts/AuthLayout';
import LoginForm from './LoginForm';

export default function AdminLogin() {
  return (
    <AuthLayout
      heading="Run campaigns with confidence."
      blurb="Manage fundraising campaigns, confirm donations and record expenses against the funds actually collected."
    >
      <LoginForm
        kind="admin"
        title="Admin sign in"
        lead="Sign in with your administrator account."
        footer={
          <>
            Are you a donor? <Link to="/donor/login">Go to the donor portal</Link>
          </>
        }
      />
    </AuthLayout>
  );
}
