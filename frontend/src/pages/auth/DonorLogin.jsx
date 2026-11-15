import { Link } from 'react-router-dom';
import AuthLayout from '../../layouts/AuthLayout';
import LoginForm from './LoginForm';

export default function DonorLogin() {
  return (
    <AuthLayout
      heading="Every contribution makes a difference."
      blurb="Browse active campaigns, donate and follow the status of your donations in one place."
    >
      <LoginForm
        kind="donor"
        title="Donor sign in"
        lead="Welcome back. Sign in to continue."
        footer={
          <>
            New here? <Link to="/donor/register">Create a donor account</Link>
            <br />
            NGO staff? <Link to="/admin/login">Admin sign in</Link>
          </>
        }
      />
    </AuthLayout>
  );
}
