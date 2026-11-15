import { CalendarDays } from 'lucide-react';
import StatusBadge from '../common/StatusBadge';
import ProgressBar from '../common/ProgressBar';
import { formatCurrency, formatDate, percent } from '../../utils/format';

// Card used for browsing campaigns. `actions` renders in the footer.
export default function CampaignCard({ campaign, showSpending = false, actions, onOpen }) {
  const pct = percent(campaign.totalCollected, campaign.goalAmount);

  return (
    <div className="card campaign-card">
      <div className="row" style={{ justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <h3 style={{ cursor: onOpen ? 'pointer' : 'default' }} onClick={onOpen}>
          {campaign.name}
        </h3>
        <StatusBadge status={campaign.status} />
      </div>

      <div>
        <div className="row" style={{ justifyContent: 'space-between', marginBottom: 6 }}>
          <span className="cell-strong">{formatCurrency(campaign.totalCollected)}</span>
          <span className="cell-muted">of {formatCurrency(campaign.goalAmount)}</span>
        </div>
        <ProgressBar value={pct} success={pct >= 100} />
        <div className="cell-muted" style={{ marginTop: 6 }}>
          {pct.toFixed(0)}% of goal collected
        </div>
      </div>

      {showSpending && (
        <dl className="kv">
          <div>
            <dt>Spent</dt>
            <dd>{formatCurrency(campaign.totalSpent)}</dd>
          </div>
          <div>
            <dt>Available funds</dt>
            <dd>{formatCurrency(campaign.availableFunds)}</dd>
          </div>
        </dl>
      )}

      <div className="row cell-muted">
        <CalendarDays size={14} />
        {formatDate(campaign.startDate)} – {formatDate(campaign.endDate)}
      </div>

      {actions && <div className="row">{actions}</div>}
    </div>
  );
}
