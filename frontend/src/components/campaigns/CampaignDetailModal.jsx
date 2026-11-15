import Modal from '../common/Modal';
import StatusBadge from '../common/StatusBadge';
import ProgressBar from '../common/ProgressBar';
import { formatCurrency, formatDate, formatDateTime, percent } from '../../utils/format';

// `showSpending` reveals spent/available (admin view). `footer` holds the actions.
export default function CampaignDetailModal({ campaign, open, onClose, showSpending = false, footer }) {
  if (!campaign) return null;
  const pct = percent(campaign.totalCollected, campaign.goalAmount);

  return (
    <Modal open={open} title={campaign.name} onClose={onClose} size="lg" footer={footer}>
      <div className="stack">
        <div className="row">
          <StatusBadge status={campaign.status} />
          <span className="cell-muted">Campaign #{campaign.campaignId}</span>
        </div>

        {campaign.description && <p>{campaign.description}</p>}

        <div>
          <div className="row" style={{ justifyContent: 'space-between', marginBottom: 6 }}>
            <span className="cell-strong">{formatCurrency(campaign.totalCollected)} collected</span>
            <span className="cell-muted">Goal {formatCurrency(campaign.goalAmount)}</span>
          </div>
          <ProgressBar value={pct} success={pct >= 100} />
          <div className="cell-muted" style={{ marginTop: 6 }}>
            {pct.toFixed(1)}% of goal
          </div>
        </div>

        <dl className="detail-grid">
          <div>
            <dt>Target amount</dt>
            <dd>{formatCurrency(campaign.goalAmount)}</dd>
          </div>
          <div>
            <dt>Current amount (collected)</dt>
            <dd>{formatCurrency(campaign.totalCollected)}</dd>
          </div>
          {showSpending && (
            <>
              <div>
                <dt>Total spent</dt>
                <dd>{formatCurrency(campaign.totalSpent)}</dd>
              </div>
              <div>
                <dt>Available funds</dt>
                <dd>{formatCurrency(campaign.availableFunds)}</dd>
              </div>
            </>
          )}
          <div>
            <dt>Start date</dt>
            <dd>{formatDate(campaign.startDate)}</dd>
          </div>
          <div>
            <dt>End date</dt>
            <dd>{formatDate(campaign.endDate)}</dd>
          </div>
          <div className="full">
            <dt>Created</dt>
            <dd>{formatDateTime(campaign.createdAt)}</dd>
          </div>
        </dl>
      </div>
    </Modal>
  );
}
