export default function LoadingSpinner({ label = 'Loading…', small = false }) {
  if (small) return <span className="spinner sm" role="status" aria-label={label} />;
  return (
    <div className="state" role="status">
      <div className="spinner" />
      <span>{label}</span>
    </div>
  );
}
