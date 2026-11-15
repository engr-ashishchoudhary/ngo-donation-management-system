import { useId } from 'react';

// options: [{ value, label }]
export default function FormSelect({
  label,
  error,
  hint,
  required,
  options = [],
  placeholder,
  className = '',
  ...props
}) {
  const id = useId();
  return (
    <div className={`field ${className}`}>
      {label && (
        <label htmlFor={id}>
          {label} {required && <span className="req">*</span>}
        </label>
      )}
      <select id={id} className={`select${error ? ' invalid' : ''}`} aria-invalid={Boolean(error)} {...props}>
        {placeholder !== undefined && <option value="">{placeholder}</option>}
        {options.map((o) => (
          <option key={o.value} value={o.value}>
            {o.label}
          </option>
        ))}
      </select>
      {error ? <span className="field-error">{error}</span> : hint && <span className="field-hint">{hint}</span>}
    </div>
  );
}
