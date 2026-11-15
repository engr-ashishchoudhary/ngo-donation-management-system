import { AlertTriangle } from 'lucide-react';

// Block-level error state with an optional retry action.
export default function ErrorMessage({ message, onRetry }) {
  return (
    <div className="state">
      <div className="state-icon danger">
        <AlertTriangle size={24} />
      </div>
      <h3>Unable to load data</h3>
      <p>{message || 'Something went wrong.'}</p>
      {onRetry && (
        <button type="button" className="btn btn-secondary btn-sm" onClick={onRetry}>
          Try again
        </button>
      )}
    </div>
  );
}
