const inr = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  maximumFractionDigits: 2,
});

export const formatCurrency = (value) => {
  if (value === null || value === undefined || value === '') return '—';
  const n = Number(value);
  return Number.isNaN(n) ? '—' : inr.format(n);
};

export const formatNumber = (value) => {
  if (value === null || value === undefined) return '—';
  return new Intl.NumberFormat('en-IN').format(Number(value));
};

// Jackson may serialise dates as ISO strings or as numeric arrays; handle both.
const toDate = (value) => {
  if (!value) return null;
  if (Array.isArray(value)) {
    const [y, m, d, h = 0, mi = 0, s = 0] = value;
    return new Date(y, m - 1, d, h, mi, s);
  }
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? null : date;
};

export const formatDate = (value) => {
  const d = toDate(value);
  return d
    ? d.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' })
    : '—';
};

export const formatDateTime = (value) => {
  const d = toDate(value);
  return d
    ? d.toLocaleString('en-IN', {
        day: '2-digit',
        month: 'short',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      })
    : '—';
};

export const toTimestamp = (value) => {
  const d = toDate(value);
  return d ? d.getTime() : 0;
};

export const toMonthKey = (value) => {
  const d = toDate(value);
  if (!d) return null;
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
};

export const monthLabel = (key) => {
  const [y, m] = key.split('-').map(Number);
  return new Date(y, m - 1, 1).toLocaleDateString('en-IN', { month: 'short', year: '2-digit' });
};

export const humanize = (value) =>
  value ? String(value).replace(/_/g, ' ').toLowerCase().replace(/^\w|\s\w/g, (c) => c.toUpperCase()) : '—';

export const percent = (part, whole) => {
  const w = Number(whole);
  if (!w) return 0;
  return Math.min(100, Math.max(0, (Number(part) / w) * 100));
};

// yyyy-MM-dd for <input type="date">
export const toInputDate = (value) => {
  const d = toDate(value);
  if (!d) return '';
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
};

export const todayInput = () => toInputDate(new Date());
