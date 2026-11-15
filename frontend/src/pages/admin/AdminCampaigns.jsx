import { useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Lock, Pencil, Plus } from 'lucide-react';
import PageHeader from '../../components/common/PageHeader';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import ErrorMessage from '../../components/common/ErrorMessage';
import ConfirmationDialog from '../../components/common/ConfirmationDialog';
import StatusBadge from '../../components/common/StatusBadge';
import ProgressBar from '../../components/common/ProgressBar';
import SearchBar from '../../components/forms/SearchBar';
import FilterDropdown from '../../components/forms/FilterDropdown';
import DataTable from '../../components/tables/DataTable';
import CampaignDetailModal from '../../components/campaigns/CampaignDetailModal';
import CampaignFormModal from '../../components/campaigns/CampaignFormModal';
import useAsync from '../../hooks/useAsync';
import { closeCampaign, getCampaigns } from '../../api/campaignApi';
import { CAMPAIGN_STATUSES } from '../../utils/constants';
import { formatCurrency, formatDate, humanize, percent } from '../../utils/format';
import { getErrorMessage } from '../../utils/errors';
import { useToast } from '../../context/ToastContext';

export default function AdminCampaigns() {
  const toast = useToast();
  const [searchParams, setSearchParams] = useSearchParams();
  const [status, setStatus] = useState('');
  const [query, setQuery] = useState('');
  const [selected, setSelected] = useState(null);
  const [formOpen, setFormOpen] = useState(searchParams.get('new') === '1');
  const [editing, setEditing] = useState(null);
  const [closing, setClosing] = useState(null);
  const [closeBusy, setCloseBusy] = useState(false);

  const { data, loading, error, reload } = useAsync(() => getCampaigns(status), [status], { initialData: [] });

  const rows = useMemo(() => {
    const q = query.trim().toLowerCase();
    return (data || []).filter((c) => !q || c.name?.toLowerCase().includes(q) || String(c.campaignId) === q);
  }, [data, query]);

  const openCreate = () => {
    setEditing(null);
    setFormOpen(true);
  };
  const closeForm = () => {
    setFormOpen(false);
    setEditing(null);
    if (searchParams.get('new')) setSearchParams({}, { replace: true });
  };

  const confirmClose = async () => {
    setCloseBusy(true);
    try {
      await closeCampaign(closing.campaignId);
      toast.success(`"${closing.name}" has been closed.`);
      setClosing(null);
      setSelected(null);
      reload();
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setCloseBusy(false);
    }
  };

  const columns = [
    {
      key: 'name',
      header: 'Campaign',
      render: (c) => (
        <>
          <div className="cell-strong">{c.name}</div>
          <div className="cell-muted">#{c.campaignId}</div>
        </>
      ),
    },
    { key: 'status', header: 'Status', render: (c) => <StatusBadge status={c.status} /> },
    {
      key: 'progress',
      header: 'Progress',
      render: (c) => (
        <div style={{ minWidth: 130 }}>
          <ProgressBar value={percent(c.totalCollected, c.goalAmount)} />
          <div className="cell-muted">{percent(c.totalCollected, c.goalAmount).toFixed(0)}%</div>
        </div>
      ),
    },
    { key: 'goalAmount', header: 'Goal', align: 'right', render: (c) => formatCurrency(c.goalAmount) },
    { key: 'totalCollected', header: 'Collected', align: 'right', render: (c) => formatCurrency(c.totalCollected) },
    { key: 'availableFunds', header: 'Available', align: 'right', render: (c) => formatCurrency(c.availableFunds) },
    {
      key: 'dates',
      header: 'Duration',
      render: (c) => (
        <span className="cell-muted">
          {formatDate(c.startDate)} – {formatDate(c.endDate)}
        </span>
      ),
    },
  ];

  const isActive = selected?.status === 'ACTIVE';

  return (
    <>
      <PageHeader
        title="Campaigns"
        subtitle="Create, review and close fundraising campaigns."
        actions={
          <button type="button" className="btn btn-primary" onClick={openCreate}>
            <Plus size={16} /> New campaign
          </button>
        }
      />

      <div className="card">
        <div className="toolbar">
          <SearchBar value={query} onChange={setQuery} placeholder="Search by name or ID" />
          <FilterDropdown
            value={status}
            onChange={setStatus}
            allLabel="All statuses"
            options={CAMPAIGN_STATUSES.map((s) => ({ value: s, label: humanize(s) }))}
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
            rowKey={(c) => c.campaignId}
            onRowClick={setSelected}
            emptyTitle="No campaigns found"
            emptyMessage={query || status ? 'Try changing your search or filter.' : 'Create your first campaign to get started.'}
          />
        )}
      </div>

      <CampaignDetailModal
        campaign={selected}
        open={Boolean(selected)}
        onClose={() => setSelected(null)}
        showSpending
        footer={
          <>
            {isActive ? (
              <>
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => {
                    setEditing(selected);
                    setSelected(null);
                    setFormOpen(true);
                  }}
                >
                  <Pencil size={15} /> Edit
                </button>
                <button type="button" className="btn btn-danger" onClick={() => setClosing(selected)}>
                  <Lock size={15} /> Close campaign
                </button>
              </>
            ) : (
              <span className="cell-muted" style={{ marginRight: 'auto' }}>
                {humanize(selected?.status)} campaigns are final and cannot be edited or re-opened.
              </span>
            )}
            <button type="button" className="btn btn-secondary" onClick={() => setSelected(null)}>
              Done
            </button>
          </>
        }
      />

      <CampaignFormModal
        open={formOpen}
        campaign={editing}
        onClose={closeForm}
        onSaved={() => {
          closeForm();
          reload();
        }}
      />

      <ConfirmationDialog
        open={Boolean(closing)}
        title="Close this campaign?"
        message={
          <>
            <p>
              <strong>{closing?.name}</strong> will stop accepting donations. Closed campaigns are final — they cannot be
              edited or re-opened.
            </p>
          </>
        }
        confirmLabel="Close campaign"
        danger
        loading={closeBusy}
        onConfirm={confirmClose}
        onCancel={() => setClosing(null)}
      />
    </>
  );
}
