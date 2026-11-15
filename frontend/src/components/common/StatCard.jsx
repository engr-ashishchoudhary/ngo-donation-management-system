export default function StatCard({ label, value, icon: Icon, tone = 'primary', hint }) {
  return (
    <div className="card stat-card">
      {Icon && (
        <div className={`stat-icon tone-${tone}`}>
          <Icon size={22} />
        </div>
      )}
      <div>
        <div className="stat-label">{label}</div>
        <div className="stat-value">{value}</div>
        {hint && <div className="stat-hint">{hint}</div>}
      </div>
    </div>
  );
}
