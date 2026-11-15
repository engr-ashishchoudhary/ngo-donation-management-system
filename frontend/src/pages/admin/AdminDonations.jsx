import { useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import PageHeader from '../../components/common/PageHeader';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import ErrorMessage from '../../components/common/ErrorMessage';
import ConfirmationDialog from '../../components/common/ConfirmationDialog';
import StatusBadge from '../../components/common/StatusBadge';
import SearchBar from '../../components/forms/SearchBar';
import FilterDropdown from '../../components/forms/FilterDropdown';
import DataTable from '../../components/tables/DataTable';
import DonationDetailModal from '../../components/donations/DonationDetailModal';
import useAsync from '../../hooks/useAsync';
import { getDonations, updateDonationStatus } from '../../api/donationApi';
import { getCampaigns } from '../../api/campaignApi';
import { DONATION_STATUSES, DONATION_TRANSITIONS } from '../../utils/constants';
import { formatCurrency, formatDateTime, humanize } from '../../utils/format';
import { getErrorMessage } from '../../utils/errors';
import { useToast } from '../../context/ToastContext';

const CONSEQUENCE = {
  COMPLETED: 'The amount will count toward the campaign’s collected funds.',
  FAILED: 'The donation will be marked as failed and will not count toward collected funds. This is final.',
  REFUNDED: 'The amount will be removed from the campaign’s collected funds. This is final.',
};

export default function AdminDonations() {
  const toast = useToast();
  const [searchParams] = useSearchParams();
  const [status, setStatus] = useState(DONATION_STATUSES.includes(searchParams.get('status')) ? searchParams.get('status') : '');
  const [campaignId, setCampaignId] = useState('');
  const [query, setQuery] = useState('');
  const [selected, setSelected] = useState(null);
  const [pendingChange, setPendingChange] = useState(null); // { donation, next }
  const [busy, setBusy] = useState(false);

  const { data, loading, error, reload } = useAsync(
    () => getDonations({ campaignId, status }),
    [campaignId, status],
    { initialData: [] }
  );
  const { data: campaigns } = useAsync(() => getCampaigns(), [], { initialData: [] });

  const rows = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return data || [];
    return (data || []).filter((d) =>
      [d.donationId, d.donorName, d.donorEmail, d.campaignName, d.transactionRef]
        .filter((v) => v !== null && v !== undefined)
        .some((v) => String(v).toLowerCase().includes(q))
    );
  }, [data, query]);

  const applyChange = async () => {
    setBusy(true);
    try {
      const updated = await updateDonationStatus(pendingChange.donation.donationId, pendingChange.next);
      toast.success(`Donation #${updated.donationId} marked as ${humanize(updated.status)}.`);
      setPendingChange(null);
      setSelected(null);
      reload();
    } catch (err) {
      toast.error(getErrorMessage(err));
      setPendingChange(null);
    } finally {
      setBusy(false);
    }
  };

  const columns = [
    { key: 'donationId', header: 'ID', render: (d) => <span className="cell-strong">#{d.donationId}</span> },
    {
      key: 'donor',
      header: 'Donor',
      render: (d) => (
        <>
          <div className="cell-strong">{d.donorName}</div>
          <div className="cell-muted">{d.donorEmail}</div>
        </>
      ),
    },
    { key: 'campaignName', header: 'Campaign' },
    { key: 'amount', header: 'Amount', align: 'right', render: (d) => formatCurrency(d.amount) },
    { key: 'paymentMethod', header: 'Method', render: (d) => humanize(d.paymentMethod) },
    { key: 'donatedAt', header: 'Date', render: (d) => <span className="cell-muted">{formatDateTime(d.donatedAt)}</span> },
    { key: 'status', header: 'Status', render: (d) => <StatusBadge status={d.status} /> },
  ];

  const transitions = selected ? DONATION_TRANSITIONS[selected.status] || [] : [];

  return (
    <>
      <PageHeader title="Donations" subtitle="Review donations and confirm their payment status." />

      <div className="card">
        <div className="toolbar">
          <SearchBar value={query} onChange={setQuery} placeholder="Search donor, campaign, reference…" />
          <FilterDropdown
            value={status}
            onChange={setStatus}
            allLabel="All statuses"
            options={DONATION_STATUSES.map((s) => ({ value: s, label: humanize(s) }))}
          />
          <FilterDropdown
            value={campaignId}
            onChange={setCampaignId}
            allLabel="All campaigns"
            options={(campaigns || []).map((c) => ({ value: String(c.campaignId), label: c.name }))}
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
            emptyMessage={query || status || campaignId ? 'Try changing your search or filters.' : 'Donations will appear here once donors contribute.'}
          />
        )}
      </div>

      <DonationDetailModal
        donation={selected}
        open={Boolean(selected)}
        onClose={() => setSelected(null)}
        footer={
          <>
            {transitions.length === 0 && (
              <span className="cell-muted" style={{ marginRight: 'auto' }}>
                {humanize(selected?.status)} donations cannot change status.
              </span>
            )}
            {transitions.map((next) => (
              <button
                key={next}
                type="button"
                className={`btn ${next === 'COMPLETED' ? 'btn-primary' : 'btn-secondary'}`}
                onClick={() => setPendingChange({ donation: selected, next })}
              >
                Mark as {humanize(next)}
              </button>
            ))}
            <button type="button" className="btn btn-secondary" onClick={() => setSelected(null)}>
              Close
            </button>
          </>
        }
      />

      <ConfirmationDialog
        open={Boolean(pendingChange)}
        title={`Mark donation as ${humanize(pendingChange?.next)}?`}
        message={
          pendingChange && (
            <>
              <p>
                Donation <strong>#{pendingChange.donation.donationId}</strong> of{' '}
                <strong>{formatCurrency(pendingChange.donation.amount)}</strong> from {pendingChange.donation.donorName}.
              </p>
              <p style={{ marginTop: 8 }}>{CONSEQUENCE[pendingChange.next]}</p>
            </>
          )
        }
        confirmLabel={`Mark as ${humanize(pendingChange?.next)}`}
        danger={pendingChange?.next === 'FAILED' || pendingChange?.next === 'REFUNDED'}
        loading={busy}
        onConfirm={applyChange}
        onCancel={() => setPendingChange(null)}
      />
    </>
  );
}
