import { useMemo, useState } from 'react';
import { Heart } from 'lucide-react';
import PageHeader from '../../components/common/PageHeader';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import ErrorMessage from '../../components/common/ErrorMessage';
import StatusBadge from '../../components/common/StatusBadge';
import SearchBar from '../../components/forms/SearchBar';
import FilterDropdown from '../../components/forms/FilterDropdown';
import DataTable from '../../components/tables/DataTable';
import DonationDetailModal from '../../components/donations/DonationDetailModal';
import DonateModal from '../../components/donations/DonateModal';
import useAsync from '../../hooks/useAsync';
import { getMyDonations } from '../../api/donationApi';
import { getCampaigns } from '../../api/campaignApi';
import { DONATION_STATUSES } from '../../utils/constants';
import { formatCurrency, formatDateTime, humanize, toTimestamp } from '../../utils/format';

export default function DonorDonations() {
  const [status, setStatus] = useState('');
  const [query, setQuery] = useState('');
  const [selected, setSelected] = useState(null);
  const [donateOpen, setDonateOpen] = useState(false);

  const { data, loading, error, reload } = useAsync(() => getMyDonations(), [], { initialData: [] });
  const { data: campaigns } = useAsync(() => getCampaigns('ACTIVE'), [], { initialData: [] });

  const rows = useMemo(() => {
    const q = query.trim().toLowerCase();
    return [...(data || [])]
      .filter(
        (d) =>
          (!status || d.status === status) &&
          (!q || [d.donationId, d.campaignName, d.transactionRef].some((v) => v !== null && v !== undefined && String(v).toLowerCase().includes(q)))
      )
      .sort((a, b) => toTimestamp(b.donatedAt) - toTimestamp(a.donatedAt));
  }, [data, status, query]);

  const columns = [
    { key: 'donationId', header: 'ID', render: (d) => <span className="cell-strong">#{d.donationId}</span> },
    { key: 'campaignName', header: 'Campaign' },
    { key: 'amount', header: 'Amount', align: 'right', render: (d) => formatCurrency(d.amount) },
    { key: 'paymentMethod', header: 'Method', render: (d) => humanize(d.paymentMethod) },
    { key: 'donatedAt', header: 'Date', render: (d) => <span className="cell-muted">{formatDateTime(d.donatedAt)}</span> },
    { key: 'status', header: 'Status', render: (d) => <StatusBadge status={d.status} /> },
  ];

  return (
    <>
      <PageHeader
        title="My Donations"
        subtitle="Your complete giving history and payment status."
        actions={
          <button type="button" className="btn btn-primary" onClick={() => setDonateOpen(true)}>
            <Heart size={16} /> Make a donation
          </button>
        }
      />

      <div className="card">
        <div className="toolbar">
          <SearchBar value={query} onChange={setQuery} placeholder="Search campaign or reference" />
          <FilterDropdown
            value={status}
            onChange={setStatus}
            allLabel="All statuses"
            options={DONATION_STATUSES.map((s) => ({ value: s, label: humanize(s) }))}
          />
        </div>
        {loading ? (
          <LoadingSpinner />
        ) : error ? (
          <ErrorMessage message={error} onRetry={reload} />
        ) : (
          <DataTable
            columns={columns}
            rows={rows}
            rowKey={(d) => d.donationId}
            onRowClick={setSelected}
            emptyTitle="No donations found"
            emptyMessage={query || status ? 'Try changing your search or filter.' : 'You have not made any donations yet.'}
          />
        )}
      </div>

      <DonationDetailModal
        donation={selected}
        open={Boolean(selected)}
        onClose={() => setSelected(null)}
        showDonor={false}
        footer={
          <button type="button" className="btn btn-secondary" onClick={() => setSelected(null)}>
            Close
          </button>
        }
      />

      <DonateModal
        open={donateOpen}
        campaigns={campaigns || []}
        onClose={() => setDonateOpen(false)}
        onDonated={() => {
          setDonateOpen(false);
          reload();
        }}
      />
    </>
  );
}
