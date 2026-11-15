import { AlertCircle, CheckCircle2, Info, TriangleAlert } from 'lucide-react';

const ICONS = { error: AlertCircle, success: CheckCircle2, info: Info, warning: TriangleAlert };

// Inline message, e.g. a form-level API error.
export default function Alert({ type = 'error', children }) {
  if (!children) return null;
  const Icon = ICONS[type];
  return (
    <div className={`alert alert-${type}`} role={type === 'error' ? 'alert' : 'status'}>
      <Icon size={16} />
      <div>{children}</div>
    </div>
  );
}
