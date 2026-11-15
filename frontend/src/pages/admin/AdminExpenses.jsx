import { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { PiggyBank, Plus, Receipt, Wallet } from 'lucide-react';
import PageHeader from '../../components/common/PageHeader';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import ErrorMessage from '../../components/common/ErrorMessage';
import EmptyState from '../../components/common/EmptyState';
import Alert from '../../components/common/Alert';
import Modal from '../../components/common/Modal';
import StatCard from '../../components/common/StatCard';
import SearchBar from '../../components/forms/SearchBar';
import FilterDropdown from '../../components/forms/FilterDropdown';
import FormInput from '../../components/forms/FormInput';
import FormSelect from '../../components/forms/FormSelect';
import FormTextarea from '../../components/forms/FormTextarea';
import DataTable from '../../components/tables/DataTable';
import useAsync from '../../hooks/useAsync';
import { getCampaigns } from '../../api/campaignApi';
import { createExpense, getExpenses } from '../../api/expenseApi';
import { EXPENSE_CATEGORIES } from '../../utils/constants';
import { formatCurrency, formatDate, humanize, todayInput, toInputDate } from '../../utils/format';
import { getErrorMessage } from '../../utils/errors';
import { useToast } from '../../context/ToastContext';

const blankForm = { description: '', amount: '', category: '', expenseDate: todayInput() };

export default function AdminExpenses() {
  const toast = useToast();
  const [searchParams] = useSearchParams();
  const [campaignId, setCampaignId] = useState(searchParams.get('campaignId') || '');
  const [category, setCategory] = useState('');
  const [query, setQuery] = useState('');
  const [selected, setSelected] = useState(null);
  const [formOpen, setFormOpen] = useState(false);
  const [values, setValues] = useState(blankForm);
  const [errors, setErrors] = useState({});
  const [apiError, setApiError] = useState('');
  const [saving, setSaving] = useState(false);

  const campaignsState = useAsync(() => getCampaigns(), [], { initialData: [] });
  const campaigns = campaignsState.data || [];

  // Default to the first campaign once the list has loaded.
  useEffect(() => {
    if (!campaignId && campaigns.length > 0) setCampaignId(String(campaigns[0].campaignId));
  }, [campaigns, campaignId]);

  const campaign = campaigns.find((c) => String(c.campaignId) === String(campaignId));

  const expensesState = useAsync(() => getExpenses(campaignId), [campaignId], {
    initialData: [],
    enabled: Boolean(campaignId),
  });

  const rows = useMemo(() => {
    const q = query.trim().toLowerCase();
    return (expensesState.data || []).filter(
      (e) =>
        (!category || e.category === category) &&
        (!q || [e.expenseId, e.description, e.adminName].some((v) => v !== null && v !== undefined && String(v).toLowerCase().includes(q)))
    );
  }, [expensesState.data, category, query]);

  const filteredTotal = rows.reduce((sum, e) => sum + Number(e.amount || 0), 0);

  const openForm = () => {
    setValues({ ...blankForm, expenseDate: todayInput() });
    setErrors({});
    setApiError('');
    setFormOpen(true);
  };

  const onChange = (e) => {
    setValues((v) => ({ ...v, [e.target.name]: e.target.value }));
    setErrors((er) => ({ ...er, [e.target.name]: undefined }));
  };

  // Mirrors ExpenseRequest + BR-15 (date not before campaign start).
  const validate = () => {
    const next = {};
    if (!values.description.trim()) next.description = 'Description is required';
    else if (values.description.trim().length > 255) next.description = 'Description must not exceed 255 characters';
    if (values.amount === '' || Number(values.amount) < 0.01) next.amount = 'Expense amount must be greater than 0';
    if (!values.category) next.category = 'Category is required';
    if (!values.expenseDate) next.expenseDate = 'Expense date is required';
    else if (campaign?.startDate && values.expenseDate < toInputDate(campaign.startDate))
      next.expenseDate = `Expense date cannot be before the campaign start date (${formatDate(campaign.startDate)})`;
    setErrors(next);
    return Object.keys(next).length === 0;
  };

  const onSubmit = async (e) => {
    e.preventDefault();
    setApiError('');
    if (!validate()) return;
    setSaving(true);
    try {
      await createExpense(campaignId, {
        campaignId: Number(campaignId),
        description: values.description.trim(),
        amount: Number(values.amount),
        category: values.category,
        expenseDate: values.expenseDate,
      });
      toast.success('Expense recorded.');
      setFormOpen(false);
      expensesState.reload();
      campaignsState.reload();
    } catch (err) {
      // Includes HTTP 422 insufficient funds — nothing was recorded.
      setApiError(getErrorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  const exceeds = campaign && values.amount !== '' && Number(values.amount) > Number(campaign.availableFunds || 0);

  const columns = [
    { key: 'expenseId', header: 'ID', render: (e) => <span className="cell-strong">#{e.expenseId}</span> },
    { key: 'campaignName', header: 'Campaign' },
    { key: 'category', header: 'Category', render: (e) => <span className="badge badge-primary">{humanize(e.category)}</span> },
    { key: 'description', header: 'Description' },
    { key: 'amount', header: 'Amount', align: 'right', render: (e) => formatCurrency(e.amount) },
    { key: 'expenseDate', header: 'Date', render: (e) => formatDate(e.expenseDate) },
    { key: 'adminName', header: 'Recorded by', render: (e) => e.adminName || '—' },
  ];

  return (
    <>
      <PageHeader
        title="Expenses"
        subtitle="Record spending against the funds a campaign has collected."
        actions={
          <button type="button" className="btn btn-primary" onClick={openForm} disabled={!campaign}>
            <Plus size={16} /> Record expense
          </button>
        }
      />

      {campaignsState.loading ? (
        <div className="card">
          <LoadingSpinner />
        </div>
      ) : campaignsState.error ? (
        <div className="card">
          <ErrorMessage message={campaignsState.error} onRetry={campaignsState.reload} />
        </div>
      ) : campaigns.length === 0 ? (
        <div className="card">
          <EmptyState title="No campaigns yet" message="Create a campaign before recording expenses." />
        </div>
      ) : (
        <>
          <div className="grid grid-stats">
            <StatCard label="Collected" value={formatCurrency(campaign?.totalCollected)} icon={Wallet} tone="success" />
            <StatCard label="Spent" value={formatCurrency(campaign?.totalSpent)} icon={Receipt} tone="danger" />
            <StatCard label="Available funds" value={formatCurrency(campaign?.availableFunds)} icon={PiggyBank} tone="primary" />
          </div>

          <div className="card">
            <div className="toolbar">
              <FormSelect
                aria-label="Campaign"
                value={campaignId}
                onChange={(e) => setCampaignId(e.target.value)}
                options={campaigns.map((c) => ({ value: String(c.campaignId), label: c.name }))}
              />
              <SearchBar value={query} onChange={setQuery} placeholder="Search description or admin" />
              <FilterDropdown
                value={category}
                onChange={setCategory}
                allLabel="All categories"
                options={EXPENSE_CATEGORIES.map((c) => ({ value: c, label: humanize(c) }))}
              />
              <span className="spacer" />
              <span className="cell-muted">Listed total: {formatCurrency(filteredTotal)}</span>
            </div>
            {expensesState.loading ? (
              <LoadingSpinner />
            ) : expensesState.error ? (
              <ErrorMessage message={expensesState.error} onRetry={expensesState.reload} />
            ) : (
              <DataTable
                columns={columns}
                rows={rows}
                rowKey={(e) => e.expenseId}
                onRowClick={setSelected}
                emptyTitle="No expenses found"
                emptyMessage={query || category ? 'Try changing your search or filter.' : 'No expenses have been recorded for this campaign.'}
              />
            )}
          </div>
        </>
      )}

      <Modal
        open={formOpen}
        title={`Record expense${campaign ? ` — ${campaign.name}` : ''}`}
        onClose={() => setFormOpen(false)}
        closeDisabled={saving}
        footer={
          <>
            <button type="button" className="btn btn-secondary" onClick={() => setFormOpen(false)} disabled={saving}>
              Cancel
            </button>
            <button type="submit" form="expense-form" className="btn btn-primary" disabled={saving}>
              {saving && <LoadingSpinner small />}
              Record expense
            </button>
          </>
        }
      >
        <form id="expense-form" onSubmit={onSubmit} noValidate className="stack" style={{ gap: 16 }}>
          <Alert type="info">
            Available funds: <strong>{formatCurrency(campaign?.availableFunds)}</strong>. Expenses are permanent and cannot be
            edited or deleted.
          </Alert>
          <Alert type="error">{apiError}</Alert>
          <div className="form-grid">
            <FormTextarea
              className="full"
              label="Description"
              name="description"
              value={values.description}
              onChange={onChange}
              error={errors.description}
              rows={2}
              maxLength={255}
              required
            />
            <FormInput
              label="Amount (₹)"
              name="amount"
              type="number"
              min="0.01"
              step="0.01"
              value={values.amount}
              onChange={onChange}
              error={errors.amount}
              hint={exceeds ? 'This exceeds the available funds and will likely be rejected.' : undefined}
              required
            />
            <FormSelect
              label="Category"
              name="category"
              value={values.category}
              onChange={onChange}
              error={errors.category}
              placeholder="Select category"
              options={EXPENSE_CATEGORIES.map((c) => ({ value: c, label: humanize(c) }))}
              required
            />
            <FormInput
              className="full"
              label="Expense date"
              name="expenseDate"
              type="date"
              value={values.expenseDate}
              onChange={onChange}
              error={errors.expenseDate}
              required
            />
          </div>
        </form>
      </Modal>

      <Modal open={Boolean(selected)} title={selected ? `Expense #${selected.expenseId}` : ''} onClose={() => setSelected(null)}>
        {selected && (
          <dl className="detail-grid">
            <div>
              <dt>Amount</dt>
              <dd>{formatCurrency(selected.amount)}</dd>
            </div>
            <div>
              <dt>Category</dt>
              <dd>{humanize(selected.category)}</dd>
            </div>
            <div>
              <dt>Campaign</dt>
              <dd>{selected.campaignName}</dd>
            </div>
            <div>
              <dt>Expense date</dt>
              <dd>{formatDate(selected.expenseDate)}</dd>
            </div>
            <div>
              <dt>Recorded by</dt>
              <dd>{selected.adminName || '—'}</dd>
            </div>
            <div>
              <dt>Recorded on</dt>
              <dd>{formatDate(selected.createdAt)}</dd>
            </div>
            <div className="full">
              <dt>Description</dt>
              <dd style={{ fontWeight: 400 }}>{selected.description}</dd>
            </div>
          </dl>
        )}
      </Modal>
    </>
  );
}
