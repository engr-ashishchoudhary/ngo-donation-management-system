import { humanize } from '../../utils/format';

// Colour mapping for every backend status enum value.
const TONES = {
  // Donation status
  COMPLETED: 'success',
  PENDING: 'warning',
  FAILED: 'danger',
  REFUNDED: 'info',
  // Campaign status (COMPLETED shared above)
  ACTIVE: 'success',
  CLOSED: 'neutral',
  // Admin roles
  SUPER_ADMIN: 'primary',
  ADMIN: 'info',
  DONOR: 'neutral',
};

export default function StatusBadge({ status, tone }) {
  if (!status) return <span className="cell-muted">—</span>;
  return <span className={`badge badge-${tone || TONES[status] || 'neutral'}`}>{humanize(status)}</span>;
}
