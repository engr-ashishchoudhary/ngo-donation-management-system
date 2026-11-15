import { useId, useState } from 'react';
import { Eye, EyeOff } from 'lucide-react';

// Labelled input with inline error. `type="password"` gets a show/hide toggle.
export default function FormInput({ label, error, hint, required, type = 'text', className = '', ...props }) {
  const id = useId();
  const [visible, setVisible] = useState(false);
  const isPassword = type === 'password';

  const input = (
    <input
      id={id}
      type={isPassword && visible ? 'text' : type}
      className={`input${error ? ' invalid' : ''}`}
      aria-invalid={Boolean(error)}
      {...props}
    />
  );

  return (
    <div className={`field ${className}`}>
      {label && (
        <label htmlFor={id}>
          {label} {required && <span className="req">*</span>}
        </label>
      )}
      {isPassword ? (
        <div className="input-wrap">
          {input}
          <button
            type="button"
            className="input-toggle"
            onClick={() => setVisible((v) => !v)}
            aria-label={visible ? 'Hide password' : 'Show password'}
            tabIndex={-1}
          >
            {visible ? <EyeOff size={18} /> : <Eye size={18} />}
          </button>
        </div>
      ) : (
        input
      )}
      {error ? <span className="field-error">{error}</span> : hint && <span className="field-hint">{hint}</span>}
    </div>
  );
}
