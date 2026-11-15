import { useId } from 'react';

export default function FormTextarea({ label, error, hint, required, className = '', ...props }) {
  const id = useId();
  return (
    <div className={`field ${className}`}>
      {label && (
        <label htmlFor={id}>
          {label} {required && <span className="req">*</span>}
        </label>
      )}
      <textarea id={id} className={`textarea${error ? ' invalid' : ''}`} aria-invalid={Boolean(error)} {...props} />
      {error ? <span className="field-error">{error}</span> : hint && <span className="field-hint">{hint}</span>}
    </div>
  );
}
