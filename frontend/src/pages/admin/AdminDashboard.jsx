import { useMemo } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import {
  Banknote,
  HandCoins,
  Megaphone,
  PiggyBank,
  Plus,
  Receipt,
  Radio,
  Users,
  Wallet,
  Clock,
} from 'lucide-react';
import PageHeader from '../../components/common/PageHeader';
import StatCard from '../../components/common/StatCard';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import ErrorMessage from '../../components/common/ErrorMessage';
import EmptyState from '../../components/common/EmptyState';
import Alert from '../../components/common/Alert';
import StatusBadge from '../../components/common/StatusBadge';
import ProgressBar from '../../components/common/ProgressBar';
import useAsync from '../../hooks/useAsync';
import { getDashboard } from '../../api/adminApi';
import { getCampaigns } from '../../api/campaignApi';
import { getDonations } from '../../api/donationApi';
import { getExpenses } from '../../api/expenseApi';
import {
  formatCurrency,
  formatDate,
  formatDateTime,
  formatNumber,
  humanize,
  monthLabel,
  percent,
  toMonthKey,
  toTimestamp,
} from '../../utils/format';

const COLORS = {
  primary: '#4f46e5',
  success: '#16a34a',
  warning: '#d97706',
  danger: '#dc2626',
  info: '#2563eb',
  grid: '#e4e8f1',
  axis: '#667085',
};

const STATUS_COLORS = {
  COMPLETED: COLORS.success,
  PENDING: COLORS.warning,
  FAILED: COLORS.danger,
  REFUNDED: COLORS.info,
};

const compact = new Intl.NumberFormat('en-IN', { notation: 'compact', maximumFractionDigits: 1 });
const axisMoney = (v) => `₹${compact.format(v)}`;
const tooltipMoney = (v) => formatCurrency(v);

// Loads everything the dashboard needs. Sections that fail individually degrade to empty states.
async function loadDashboard() {
  const [dashboard, campaigns, donations] = await Promise.allSettled([getDashboard(), getCampaigns(), getDonations()]);
  if (dashboard.status === 'rejected') throw dashboard.reason;

  const campaignList = campaigns.status === 'fulfilled' ? campaigns.value : [];
  const expenseResults = await Promise.allSettled(campaignList.map((c) => getExpenses(c.campaignId)));
  const expenses = expenseResults.flatMap((r) => (r.status === 'fulfilled' ? r.value : []));

  const failed = [];
  if (campaigns.status === 'rejected') failed.push('campaigns');
  if (donations.status === 'rejected') failed.push('donations');
  if (expenseResults.some((r) => r.status === 'rejected')) failed.push('expenses');

  return {
    dashboard: dashboard.value,
    campaigns: campaignList,
    donations: donations.status === 'fulfilled' ? donations.value : [],
    expenses,
    failed,
  };
}

function ChartCard({ title, subtitle, empty, emptyMessage, children }) {
  return (
    <div className="card">
      <div className="card-header">
        <div>
          <div className="card-title">{title}</div>
          {subtitle && <div className="card-subtitle">{subtitle}</div>}
        </div>
      </div>
      {empty ? <EmptyState title="No data yet" message={emptyMessage} /> : children}
    </div>
  );
}

export default function AdminDashboard() {
  const navigate = useNavigate();
  const { data, loading, error, reload } = useAsync(loadDashboard, []);

  const derived = useMemo(() => {
    if (!data) return null;
    const { dashboard, campaigns, donations, expenses } = data;

    // Completed donation amounts grouped by month.
    const byMonth = {};
    donations
      .filter((d) => d.status === 'COMPLETED')
      .forEach((d) => {
        const key = toMonthKey(d.donatedAt);
        if (key) byMonth[key] = (byMonth[key] || 0) + Number(d.amount || 0);
      });
    const trend = Object.keys(byMonth)
      .sort()
      .map((k) => ({ month: monthLabel(k), amount: byMonth[k] }));

    const performance = [...campaigns]
      .sort((a, b) => Number(b.totalCollected) - Number(a.totalCollected))
      .slice(0, 6)
      .map((c) => ({
        name: c.name.length > 16 ? `${c.name.slice(0, 15)}…` : c.name,
        Goal: Number(c.goalAmount),
        Collected: Number(c.totalCollected),
      }));

    const statusData = [
      { key: 'COMPLETED', value: Number(dashboard.totalCompletedDonations || 0) },
      { key: 'PENDING', value: Number(dashboard.totalPendingDonations || 0) },
      { key: 'FAILED', value: Number(dashboard.totalFailedDonations || 0) },
      { key: 'REFUNDED', value: Number(dashboard.totalRefundedDonations || 0) },
    ]
      .filter((s) => s.value > 0)
      .map((s) => ({ ...s, name: humanize(s.key) }));

    const byCategory = {};
    expenses.forEach((e) => {
      byCategory[e.category] = (byCategory[e.category] || 0) + Number(e.amount || 0);
    });
    const expenseData = Object.entries(byCategory).map(([category, amount]) => ({ category: humanize(category), amount }));

    const recentDonations = [...donations].sort((a, b) => toTimestamp(b.donatedAt) - toTimestamp(a.donatedAt)).slice(0, 5);
    const recentExpenses = [...expenses]
      .sort((a, b) => toTimestamp(b.createdAt) - toTimestamp(a.createdAt) || b.expenseId - a.expenseId)
      .slice(0, 5);
    const activeCampaigns = campaigns.filter((c) => c.status === 'ACTIVE').slice(0, 5);
    const totalDonations =
      Number(dashboard.totalCompletedDonations || 0) +
      Number(dashboard.totalPendingDonations || 0) +
      Number(dashboard.totalFailedDonations || 0) +
      Number(dashboard.totalRefundedDonations || 0);

    return { trend, performance, statusData, expenseData, recentDonations, recentExpenses, activeCampaigns, totalDonations };
  }, [data]);

  if (loading) return <LoadingSpinner label="Loading dashboard…" />;
  if (error) return <ErrorMessage message={error} onRetry={reload} />;

  const { dashboard, failed } = data;

  return (
    <>
      <PageHeader
        title="Dashboard"
        subtitle="A live overview of donations, campaigns and spending."
        actions={
          <>
            <button type="button" className="btn btn-secondary" onClick={() => navigate('/admin/donations?status=PENDING')}>
              <Clock size={16} /> Review pending
            </button>
            <button type="button" className="btn btn-secondary" onClick={() => navigate('/admin/expenses')}>
              <Receipt size={16} /> Record expense
            </button>
            <button type="button" className="btn btn-primary" onClick={() => navigate('/admin/campaigns?new=1')}>
              <Plus size={16} /> New campaign
            </button>
          </>
        }
      />

      {failed.length > 0 && (
        <div className="mb-20">
          <Alert type="warning">Some sections could not be loaded ({failed.join(', ')}). Figures below may be incomplete.</Alert>
        </div>
      )}

      <div className="grid grid-stats">
        <StatCard label="Total donations" value={formatNumber(derived.totalDonations)} icon={HandCoins} tone="primary" hint={`${formatNumber(dashboard.totalCompletedDonations)} completed`} />
        <StatCard label="Funds collected" value={formatCurrency(dashboard.grandTotalCollected)} icon={Wallet} tone="success" hint="Completed donations only" />
        <StatCard label="Total expenses" value={formatCurrency(dashboard.grandTotalSpent)} icon={Banknote} tone="danger" />
        <StatCard label="Available funds" value={formatCurrency(dashboard.grandTotalAvailable)} icon={PiggyBank} tone="info" />
        <StatCard label="Total campaigns" value={formatNumber(dashboard.totalCampaigns)} icon={Megaphone} tone="primary" />
        <StatCard label="Active campaigns" value={formatNumber(dashboard.activeCampaignsCount)} icon={Radio} tone="success" />
        <StatCard label="Active donors" value={formatNumber(dashboard.totalActiveDonors)} icon={Users} tone="warning" />
      </div>

      <div className="grid grid-2 mb-20">
        <ChartCard
          title="Donation trends"
          subtitle="Completed donations per month"
          empty={derived.trend.length === 0}
          emptyMessage="Completed donations will appear here as a monthly trend."
        >
          <div className="chart-box">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={derived.trend} margin={{ left: 4, right: 12, top: 8 }}>
                <defs>
                  <linearGradient id="trendFill" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor={COLORS.primary} stopOpacity={0.25} />
                    <stop offset="100%" stopColor={COLORS.primary} stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid stroke={COLORS.grid} vertical={false} />
                <XAxis dataKey="month" tick={{ fill: COLORS.axis, fontSize: 12 }} axisLine={false} tickLine={false} />
                <YAxis tickFormatter={axisMoney} tick={{ fill: COLORS.axis, fontSize: 12 }} axisLine={false} tickLine={false} width={64} />
                <Tooltip formatter={tooltipMoney} />
                <Area type="monotone" dataKey="amount" name="Collected" stroke={COLORS.primary} strokeWidth={2} fill="url(#trendFill)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </ChartCard>

        <ChartCard
          title="Campaign performance"
          subtitle="Goal vs collected (top campaigns)"
          empty={derived.performance.length === 0}
          emptyMessage="Create a campaign to track its performance."
        >
          <div className="chart-box">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={derived.performance} margin={{ left: 4, right: 12, top: 8 }}>
                <CartesianGrid stroke={COLORS.grid} vertical={false} />
                <XAxis dataKey="name" tick={{ fill: COLORS.axis, fontSize: 12 }} axisLine={false} tickLine={false} />
                <YAxis tickFormatter={axisMoney} tick={{ fill: COLORS.axis, fontSize: 12 }} axisLine={false} tickLine={false} width={64} />
                <Tooltip formatter={tooltipMoney} />
                <Legend />
                <Bar dataKey="Goal" fill="#c7cbf5" radius={[4, 4, 0, 0]} />
                <Bar dataKey="Collected" fill={COLORS.success} radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </ChartCard>

        <ChartCard
          title="Donation status distribution"
          subtitle="Number of donations by status"
          empty={derived.statusData.length === 0}
          emptyMessage="No donations have been recorded yet."
        >
          <div className="chart-box">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie data={derived.statusData} dataKey="value" nameKey="name" innerRadius={62} outerRadius={96} paddingAngle={2}>
                  {derived.statusData.map((s) => (
                    <Cell key={s.key} fill={STATUS_COLORS[s.key]} />
                  ))}
                </Pie>
                <Tooltip />
                <Legend />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </ChartCard>

        <ChartCard
          title="Expense overview"
          subtitle="Spending by category across campaigns"
          empty={derived.expenseData.length === 0}
          emptyMessage="Recorded expenses will be summarised here."
        >
          <div className="chart-box">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={derived.expenseData} margin={{ left: 4, right: 12, top: 8 }}>
                <CartesianGrid stroke={COLORS.grid} vertical={false} />
                <XAxis dataKey="category" tick={{ fill: COLORS.axis, fontSize: 12 }} axisLine={false} tickLine={false} />
                <YAxis tickFormatter={axisMoney} tick={{ fill: COLORS.axis, fontSize: 12 }} axisLine={false} tickLine={false} width={64} />
                <Tooltip formatter={tooltipMoney} />
                <Bar dataKey="amount" name="Spent" fill={COLORS.danger} radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </ChartCard>
      </div>

      <div className="grid grid-3">
        <div className="card">
          <div className="card-header">
            <div className="card-title">Recent donations</div>
            <Link to="/admin/donations">View all</Link>
          </div>
          {derived.recentDonations.length === 0 ? (
            <EmptyState title="No donations yet" />
          ) : (
            <ul className="list">
              {derived.recentDonations.map((d) => (
                <li key={d.donationId}>
                  <div>
                    <div className="primary">{d.donorName}</div>
                    <div className="secondary">
                      {d.campaignName} · {formatDateTime(d.donatedAt)}
                    </div>
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
            <Link to="/admin/campaigns">View all</Link>
          </div>
          {derived.activeCampaigns.length === 0 ? (
            <EmptyState title="No active campaigns" />
          ) : (
            <ul className="list">
              {derived.activeCampaigns.map((c) => (
                <li key={c.campaignId} style={{ display: 'block' }}>
                  <div className="row" style={{ justifyContent: 'space-between' }}>
                    <span className="primary">{c.name}</span>
                    <span className="secondary">{percent(c.totalCollected, c.goalAmount).toFixed(0)}%</span>
                  </div>
                  <ProgressBar value={percent(c.totalCollected, c.goalAmount)} />
                  <div className="secondary" style={{ marginTop: 4 }}>
                    {formatCurrency(c.totalCollected)} of {formatCurrency(c.goalAmount)} · ends {formatDate(c.endDate)}
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>

        <div className="card">
          <div className="card-header">
            <div className="card-title">Recent expenses</div>
            <Link to="/admin/expenses">View all</Link>
          </div>
          {derived.recentExpenses.length === 0 ? (
            <EmptyState title="No expenses recorded" />
          ) : (
            <ul className="list">
              {derived.recentExpenses.map((e) => (
                <li key={e.expenseId}>
                  <div>
                    <div className="primary">{e.description}</div>
                    <div className="secondary">
                      {e.campaignName} · {humanize(e.category)} · {formatDate(e.expenseDate)}
                    </div>
                  </div>
                  <div className="primary">{formatCurrency(e.amount)}</div>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </>
  );
}
