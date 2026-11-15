import Modal from '../common/Modal';
import StatusBadge from '../common/StatusBadge';
import { formatCurrency, formatDateTime, humanize } from '../../utils/format';

// Read-only donation detail; `footer` carries admin actions when needed.
export default function DonationDetailModal({ donation, open, onClose, showDonor = true, footer }) {
  if (!donation) return null;
  return (
    <Modal open={open} title={`Donation #${donation.donationId}`} onClose={onClose} footer={footer}>
      <dl className="detail-grid">
        <div>
          <dt>Amount</dt>
          <dd>{formatCurrency(donation.amount)}</dd>
        </div>
        <div>
          <dt>Status</dt>
          <dd>
            <StatusBadge status={donation.status} />
          </dd>
        </div>
        {showDonor && (
          <>
            <div>
              <dt>Donor</dt>
              <dd>{donation.donorName || '—'}</dd>
            </div>
            <div>
              <dt>Donor email</dt>
              <dd>{donation.donorEmail || '—'}</dd>
            </div>
          </>
        )}
        <div>
          <dt>Campaign</dt>
          <dd>{donation.campaignName || '—'}</dd>
        </div>
        <div>
          <dt>Campaign status</dt>
          <dd>
            <StatusBadge status={donation.campaignStatus} />
          </dd>
        </div>
        <div>
          <dt>Payment method</dt>
          <dd>{humanize(donation.paymentMethod)}</dd>
        </div>
        <div>
          <dt>Date</dt>
          <dd>{formatDateTime(donation.donatedAt)}</dd>
        </div>
        <div className="full">
          <dt>Transaction reference</dt>
          <dd>{donation.transactionRef || '—'}</dd>
        </div>
        <div className="full">
          <dt>Notes</dt>
          <dd style={{ fontWeight: 400 }}>{donation.notes || '—'}</dd>
        </div>
      </dl>
    </Modal>
  );
}
