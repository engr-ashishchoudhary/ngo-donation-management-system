import { useMemo, useState } from 'react';
import PageHeader from '../../components/common/PageHeader';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import ErrorMessage from '../../components/common/ErrorMessage';
import Alert from '../../components/common/Alert';
import Modal from '../../components/common/Modal';
import SearchBar from '../../components/forms/SearchBar';
import DataTable from '../../components/tables/DataTable';
import StatusBadge from '../../components/common/StatusBadge';
import useAsync from '../../hooks/useAsync';
import { getDonors } from '../../api/adminApi';
import { getDonations } from '../../api/donationApi';
import { formatCurrency, formatDate, formatDateTime } from '../../utils/format';

export default function AdminDonors() {
  const [query, setQuery] = useState('');
  const [selected, setSelected] = useState(null);

  const donorsState = useAsync(() => getDonors(), [], { initialData: [] });
  // Donation history is derived from the admin donations list (filtered by donorId).
  const donationsState = useAsync(() => getDonations(), [], { initialData: [] });

  const rows = useMemo(() => {
    const q = query.trim().toLowerCase();
    return (donorsState.data || []).filter(
      (d) => !q || [d.name, d.email, d.phone, d.donorId].some((v) => v !== null && v !== undefined && String(v).toLowerCase().includes(q))
    );
  }, [donorsState.data, query]);

  const history = useMemo(
    () => (selected ? (donationsState.data || []).filter((d) => d.donorId === selected.donorId) : []),
    [donationsState.data, selected]
  );
  const totalGiven = history.filter((d) => d.status === 'COMPLETED').reduce((s, d) => s + Number(d.amount || 0), 0);

  const columns = [
    {
      key: 'name',
      header: 'Donor',
      render: (d) => (
        <>
          <div className="cell-strong">{d.name}</div>
          <div className="cell-muted">#{d.donorId}</div>
        </>
      ),
    },
    { key: 'email', header: 'Email' },
    { key: 'phone', header: 'Phone', render: (d) => d.phone || '—' },
    {
      key: 'isActive',
      header: 'Account',
      render: (d) => <StatusBadge status={d.isActive ? 'ACTIVE' : 'CLOSED'} tone={d.isActive ? 'success' : 'neutral'} />,
    },
    { key: 'createdAt', header: 'Registered', render: (d) => <span className="cell-muted">{formatDate(d.createdAt)}</span> },
  ];

  const historyColumns = [
    { key: 'donationId', header: 'ID', render: (d) => `#${d.donationId}` },
    { key: 'campaignName', header: 'Campaign' },
    { key: 'amount', header: 'Amount', align: 'right', render: (d) => formatCurrency(d.amount) },
    { key: 'status', header: 'Status', render: (d) => <StatusBadge status={d.status} /> },
    { key: 'donatedAt', header: 'Date', render: (d) => <span className="cell-muted">{formatDateTime(d.donatedAt)}</span> },
  ];

  return (
    <>
      <PageHeader title="Donors" subtitle="Registered donors and their giving history." />

      <div className="card">
        <div className="toolbar">
          <SearchBar value={query} onChange={setQuery} placeholder="Search name, email or phone" />
        </div>
        {donorsState.loading ? (
          <LoadingSpinner />
        ) : donorsState.error ? (
          <ErrorMessage message={donorsState.error} onRetry={donorsState.reload} />
        ) : (
          <DataTable
            columns={columns}
            rows={rows}
            rowKey={(d) => d.donorId}
            onRowClick={setSelected}
            emptyTitle="No donors found"
            emptyMessage={query ? 'Try a different search.' : 'Donors will appear here once they register.'}
          />
        )}
      </div>

      <Modal open={Boolean(selected)} title={selected?.name || ''} onClose={() => setSelected(null)} size="lg">
        {selected && (
          <div className="stack">
            <dl className="detail-grid">
              <div>
                <dt>Email</dt>
                <dd>{selected.email}</dd>
              </div>
              <div>
                <dt>Phone</dt>
                <dd>{selected.phone || '—'}</dd>
              </div>
              <div>
                <dt>Registered</dt>
                <dd>{formatDate(selected.createdAt)}</dd>
              </div>
              <div>
                <dt>Total donated (completed)</dt>
                <dd>{donationsState.error ? '—' : formatCurrency(totalGiven)}</dd>
              </div>
              <div className="full">
                <dt>Address</dt>
                <dd style={{ fontWeight: 400 }}>{selected.address || '—'}</dd>
              </div>
            </dl>

            <div>
              <h3 style={{ fontSize: 15, marginBottom: 10 }}>Donation history</h3>
              {donationsState.loading ? (
                <LoadingSpinner />
              ) : donationsState.error ? (
                <Alert type="error">{donationsState.error}</Alert>
              ) : (
                <div className="card">
                  <DataTable
                    columns={historyColumns}
                    rows={history}
                    rowKey={(d) => d.donationId}
                    pageSize={5}
                    emptyTitle="No donations yet"
                  />
                </div>
              )}
            </div>
          </div>
        )}
      </Modal>
    </>
  );
}
