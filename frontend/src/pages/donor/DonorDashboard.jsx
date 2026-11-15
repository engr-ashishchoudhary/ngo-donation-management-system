import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { Clock, HandCoins, Heart, Megaphone, Wallet } from 'lucide-react';
import PageHeader from '../../components/common/PageHeader';
import StatCard from '../../components/common/StatCard';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import ErrorMessage from '../../components/common/ErrorMessage';
import EmptyState from '../../components/common/EmptyState';
import StatusBadge from '../../components/common/StatusBadge';
import CampaignCard from '../../components/campaigns/CampaignCard';
import DonateModal from '../../components/donations/DonateModal';
import useAsync from '../../hooks/useAsync';
import { getMyDonations } from '../../api/donationApi';
import { getCampaigns } from '../../api/campaignApi';
import { useAuth } from '../../context/AuthContext';
import { formatCurrency, formatDateTime, formatNumber, toTimestamp } from '../../utils/format';

async function load() {
  const [donations, campaigns] = await Promise.all([getMyDonations(), getCampaigns('ACTIVE')]);
  return { donations, campaigns };
}

export default function DonorDashboard() {
  const { user } = useAuth();
  const { data, loading, error, reload } = useAsync(load, []);
  const [donateFor, setDonateFor] = useState(null); // null = closed, 0 = open without preset, id = preset
  const [donateOpen, setDonateOpen] = useState(false);

  const stats = useMemo(() => {
    if (!data) return null;
    const mine = data.donations;
    const completed = mine.filter((d) => d.status === 'COMPLETED');
    return {
      totalGiven: completed.reduce((s, d) => s + Number(d.amount || 0), 0),
      count: mine.length,
      completed: completed.length,
      pending: mine.filter((d) => d.status === 'PENDING').length,
      recent: [...mine].sort((a, b) => toTimestamp(b.donatedAt) - toTimestamp(a.donatedAt)).slice(0, 5),
      campaignsSupported: new Set(completed.map((d) => d.campaignId)).size,
    };
  }, [data]);

  if (loading) return <LoadingSpinner label="Loading your dashboard…" />;
  if (error) return <ErrorMessage message={error} onRetry={reload} />;

  const openDonate = (campaignId) => {
    setDonateFor(campaignId || null);
    setDonateOpen(true);
  };

  return (
    <>
      <PageHeader
        title={`Welcome, ${user?.name?.split(' ')[0] || 'friend'}`}
        subtitle="Here is a summary of your giving."
        actions={
          <button type="button" className="btn btn-primary" onClick={() => openDonate(null)}>
            <Heart size={16} /> Make a donation
          </button>
        }
      />

      <div className="grid grid-stats">
        <StatCard label="Total donated" value={formatCurrency(stats.totalGiven)} icon={Wallet} tone="success" hint="Completed donations" />
        <StatCard label="Donations made" value={formatNumber(stats.count)} icon={HandCoins} tone="primary" hint={`${stats.completed} completed`} />
        <StatCard label="Pending confirmation" value={formatNumber(stats.pending)} icon={Clock} tone="warning" />
        <StatCard label="Campaigns supported" value={formatNumber(stats.campaignsSupported)} icon={Megaphone} tone="info" />
      </div>

      <div className="grid grid-2 mb-20" style={{ alignItems: 'start' }}>
        <div className="card">
          <div className="card-header">
            <div className="card-title">Recent donations</div>
            <Link to="/donor/donations">View all</Link>
          </div>
          {stats.recent.length === 0 ? (
            <EmptyState title="No donations yet" message="Your donations will appear here." />
          ) : (
            <ul className="list">
              {stats.recent.map((d) => (
                <li key={d.donationId}>
                  <div>
                    <div className="primary">{d.campaignName}</div>
                    <div className="secondary">{formatDateTime(d.donatedAt)}</div>
                  </div>
                  <div style={{ textAlign: 'right' }}>
                    <div className="primary">{formatCurrency(d.amount)}</div>
                    <StatusBadge status={d.status} />
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>

        <div className="card">
          <div className="card-header">
            <div className="card-title">Active campaigns</div>
            <Link to="/donor/campaigns">Browse all</Link>
          </div>
          {data.campaigns.length === 0 ? (
            <EmptyState title="No active campaigns" message="Check back soon." />
          ) : (
            <ul className="list">
              {data.campaigns.slice(0, 5).map((c) => (
                <li key={c.campaignId}>
                  <div>
                    <div className="primary">{c.name}</div>
                    <div className="secondary">
                      {formatCurrency(c.totalCollected)} of {formatCurrency(c.goalAmount)}
                    </div>
                  </div>
                  <button type="button" className="btn btn-secondary btn-sm" onClick={() => openDonate(c.campaignId)}>
                    Donate
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>

      {data.campaigns.length > 0 && (
        <>
          <h3 style={{ marginBottom: 14 }}>Featured campaigns</h3>
          <div className="grid grid-cards">
            {data.campaigns.slice(0, 3).map((c) => (
              <CampaignCard
                key={c.campaignId}
                campaign={c}
                actions={
                  <button type="button" className="btn btn-primary btn-sm" onClick={() => openDonate(c.campaignId)}>
                    <Heart size={14} /> Donate
                  </button>
                }
              />
            ))}
          </div>
        </>
      )}

      <DonateModal
        open={donateOpen}
        campaigns={data.campaigns}
        defaultCampaignId={donateFor}
        onClose={() => setDonateOpen(false)}
        onDonated={() => {
          setDonateOpen(false);
          reload();
        }}
      />
    </>
  );
}
