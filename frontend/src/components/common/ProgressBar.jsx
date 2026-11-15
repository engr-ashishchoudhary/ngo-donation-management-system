export default function ProgressBar({ value, success = false }) {
  const pct = Math.min(100, Math.max(0, Number(value) || 0));
  return (
    <div
      className={`progress${success ? ' success' : ''}`}
      role="progressbar"
      aria-valuenow={Math.round(pct)}
      aria-valuemin={0}
      aria-valuemax={100}
    >
      <span style={{ width: `${pct}%` }} />
    </div>
  );
}
