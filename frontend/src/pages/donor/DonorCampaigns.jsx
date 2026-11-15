import { useMemo, useState } from 'react';
import { Heart } from 'lucide-react';
import PageHeader from '../../components/common/PageHeader';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import ErrorMessage from '../../components/common/ErrorMessage';
import EmptyState from '../../components/common/EmptyState';
import SearchBar from '../../components/forms/SearchBar';
import FilterDropdown from '../../components/forms/FilterDropdown';
import CampaignCard from '../../components/campaigns/CampaignCard';
import CampaignDetailModal from '../../components/campaigns/CampaignDetailModal';
import DonateModal from '../../components/donations/DonateModal';
import useAsync from '../../hooks/useAsync';
import { getCampaigns } from '../../api/campaignApi';
import { CAMPAIGN_STATUSES } from '../../utils/constants';
import { humanize } from '../../utils/format';

export default function DonorCampaigns() {
  const [status, setStatus] = useState('ACTIVE');
  const [query, setQuery] = useState('');
  const [selected, setSelected] = useState(null);
  const [donateOpen, setDonateOpen] = useState(false);
  const [donateFor, setDonateFor] = useState(null);

  const { data, loading, error, reload } = useAsync(() => getCampaigns(status), [status], { initialData: [] });
  // The donate form always needs the ACTIVE set, whatever the browse filter is.
  const { data: activeCampaigns, reload: reloadActive } = useAsync(() => getCampaigns('ACTIVE'), [], { initialData: [] });

  const rows = useMemo(() => {
    const q = query.trim().toLowerCase();
    return (data || []).filter((c) => !q || c.name?.toLowerCase().includes(q));
  }, [data, query]);

  const openDonate = (id) => {
    setDonateFor(id);
    setDonateOpen(true);
  };

  return (
    <>
      <PageHeader title="Browse Campaigns" subtitle="Find a cause and contribute." />

      <div className="card mb-20">
        <div className="toolbar" style={{ borderBottom: 'none' }}>
          <SearchBar value={query} onChange={setQuery} placeholder="Search campaigns" />
          <FilterDropdown
            value={status}
            onChange={setStatus}
            allLabel="All statuses"
            options={CAMPAIGN_STATUSES.map((s) => ({ value: s, label: humanize(s) }))}
          />
        </div>
      </div>

      {loading ? (
        <LoadingSpinner />
      ) : error ? (
        <div className="card">
          <ErrorMessage message={error} onRetry={reload} />
        </div>
      ) : rows.length === 0 ? (
        <div className="card">
          <EmptyState title="No campaigns found" message={query ? 'Try a different search.' : 'There are no campaigns to show right now.'} />
        </div>
      ) : (
        <div className="grid grid-cards">
          {rows.map((c) => (
            <CampaignCard
              key={c.campaignId}
              campaign={c}
              onOpen={() => setSelected(c)}
              actions={
                <>
                  <button type="button" className="btn btn-secondary btn-sm" onClick={() => setSelected(c)}>
                    Details
                  </button>
                  {c.status === 'ACTIVE' && (
                    <button type="button" className="btn btn-primary btn-sm" onClick={() => openDonate(c.campaignId)}>
                      <Heart size={14} /> Donate
                    </button>
                  )}
                </>
              }
            />
          ))}
        </div>
      )}

      <CampaignDetailModal
        campaign={selected}
        open={Boolean(selected)}
        onClose={() => setSelected(null)}
        footer={
          <>
            <button type="button" className="btn btn-secondary" onClick={() => setSelected(null)}>
              Close
            </button>
            {selected?.status === 'ACTIVE' && (
              <button
                type="button"
                className="btn btn-primary"
                onClick={() => {
                  const id = selected.campaignId;
                  setSelected(null);
                  openDonate(id);
                }}
              >
                <Heart size={15} /> Donate
              </button>
            )}
          </>
        }
      />

      <DonateModal
        open={donateOpen}
        campaigns={activeCampaigns || []}
        defaultCampaignId={donateFor}
        onClose={() => setDonateOpen(false)}
        onDonated={() => {
          setDonateOpen(false);
          reload();
          reloadActive();
        }}
      />
    </>
  );
}
