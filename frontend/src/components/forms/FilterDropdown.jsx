// options: [{ value, label }]; empty string means "all".
export default function FilterDropdown({ value, onChange, options, allLabel = 'All', label }) {
  return (
    <select
      className="select filter"
      value={value}
      onChange={(e) => onChange(e.target.value)}
      aria-label={label || allLabel}
    >
      <option value="">{allLabel}</option>
      {options.map((o) => (
        <option key={o.value} value={o.value}>
          {o.label}
        </option>
      ))}
    </select>
  );
}
