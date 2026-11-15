import { HeartHandshake } from 'lucide-react';

// Split-screen shell for login / registration pages.
export default function AuthLayout({ heading, blurb, children }) {
  return (
    <div className="auth-page">
      <aside className="auth-aside">
        <div className="row" style={{ color: '#fff', fontWeight: 700, fontSize: 16 }}>
          <span className="brand-mark">
            <HeartHandshake size={20} />
          </span>
          NGO Donations
        </div>
        <div>
          <h2>{heading}</h2>
          <p>{blurb}</p>
        </div>
        <small style={{ color: '#7e8ac0' }}>Transparent campaigns. Auditable funds.</small>
      </aside>
      <main className="auth-main">{children}</main>
    </div>
  );
}
