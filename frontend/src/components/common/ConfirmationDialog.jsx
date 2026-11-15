import Modal from './Modal';
import LoadingSpinner from './LoadingSpinner';

export default function ConfirmationDialog({
  open,
  title,
  message,
  confirmLabel = 'Confirm',
  cancelLabel = 'Cancel',
  danger = false,
  loading = false,
  onConfirm,
  onCancel,
}) {
  return (
    <Modal
      open={open}
      title={title}
      onClose={onCancel}
      size="sm"
      closeDisabled={loading}
      footer={
        <>
          <button type="button" className="btn btn-secondary" onClick={onCancel} disabled={loading}>
            {cancelLabel}
          </button>
          <button
            type="button"
            className={`btn ${danger ? 'btn-danger' : 'btn-primary'}`}
            onClick={onConfirm}
            disabled={loading}
          >
            {loading && <LoadingSpinner small />}
            {confirmLabel}
          </button>
        </>
      }
    >
      <div>{message}</div>
    </Modal>
  );
}
