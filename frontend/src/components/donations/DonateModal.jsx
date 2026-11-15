import { useEffect, useState } from 'react';
import Modal from '../common/Modal';
import Alert from '../common/Alert';
import LoadingSpinner from '../common/LoadingSpinner';
import FormInput from '../forms/FormInput';
import FormSelect from '../forms/FormSelect';
import FormTextarea from '../forms/FormTextarea';
import { createDonation } from '../../api/donationApi';
import { PAYMENT_METHODS } from '../../utils/constants';
import { getErrorMessage } from '../../utils/errors';
import { humanize } from '../../utils/format';
import { useToast } from '../../context/ToastContext';

// Records a donation via POST /api/donations (no payment gateway is involved).
// `campaigns` should contain ACTIVE campaigns only — the backend rejects the rest.
export default function DonateModal({ open, campaigns, defaultCampaignId, onClose, onDonated }) {
  const toast = useToast();
  const [values, setValues] = useState({ campaignId: '', amount: '', paymentMethod: '', transactionRef: '', notes: '' });
  const [errors, setErrors] = useState({});
  const [apiError, setApiError] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!open) return;
    setErrors({});
    setApiError('');
    setValues({
      campaignId: defaultCampaignId ? String(defaultCampaignId) : '',
      amount: '',
      paymentMethod: '',
      transactionRef: '',
      notes: '',
    });
  }, [open, defaultCampaignId]);

  const onChange = (e) => {
    setValues((v) => ({ ...v, [e.target.name]: e.target.value }));
    setErrors((er) => ({ ...er, [e.target.name]: undefined }));
  };

  const validate = () => {
    const next = {};
    if (!values.campaignId) next.campaignId = 'Select a campaign';
    if (values.amount === '' || Number(values.amount) < 0.01) next.amount = 'Donation amount must be greater than 0';
    if (!values.paymentMethod) next.paymentMethod = 'Select a payment method';
    if (values.transactionRef.trim().length > 100) next.transactionRef = 'Transaction reference must not exceed 100 characters';
    setErrors(next);
    return Object.keys(next).length === 0;
  };

  const onSubmit = async (e) => {
    e.preventDefault();
    setApiError('');
    if (!validate()) return;
    setSaving(true);
    try {
      await createDonation({
        campaignId: Number(values.campaignId),
        amount: Number(values.amount),
        paymentMethod: values.paymentMethod,
        transactionRef: values.transactionRef.trim() || null,
        notes: values.notes.trim() || null,
      });
      toast.success('Donation recorded. It will count toward the campaign once it is confirmed (completed).');
      onDonated();
    } catch (err) {
      setApiError(getErrorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal
      open={open}
      title="Make a donation"
      onClose={onClose}
      closeDisabled={saving}
      footer={
        <>
          <button type="button" className="btn btn-secondary" onClick={onClose} disabled={saving}>
            Cancel
          </button>
          <button type="submit" form="donate-form" className="btn btn-primary" disabled={saving || campaigns.length === 0}>
            {saving && <LoadingSpinner small />}
            Submit donation
          </button>
        </>
      }
    >
      <form id="donate-form" onSubmit={onSubmit} noValidate className="stack" style={{ gap: 16 }}>
        <Alert type="info">
          Donations are recorded as <strong>Pending</strong> until an administrator confirms the payment.
        </Alert>
        {campaigns.length === 0 && <Alert type="warning">There are no active campaigns accepting donations right now.</Alert>}
        <Alert type="error">{apiError}</Alert>
        <div className="form-grid">
          <FormSelect
            className="full"
            label="Campaign"
            name="campaignId"
            value={values.campaignId}
            onChange={onChange}
            error={errors.campaignId}
            placeholder="Select a campaign"
            options={campaigns.map((c) => ({ value: String(c.campaignId), label: c.name }))}
            required
          />
          <FormInput
            label="Amount (₹)"
            name="amount"
            type="number"
            min="0.01"
            step="0.01"
            value={values.amount}
            onChange={onChange}
            error={errors.amount}
            required
          />
          <FormSelect
            label="Payment method"
            name="paymentMethod"
            value={values.paymentMethod}
            onChange={onChange}
            error={errors.paymentMethod}
            placeholder="Select method"
            options={PAYMENT_METHODS.map((m) => ({ value: m, label: humanize(m) }))}
            required
          />
          <FormInput
            className="full"
            label="Transaction reference"
            name="transactionRef"
            value={values.transactionRef}
            onChange={onChange}
            error={errors.transactionRef}
            maxLength={100}
            hint="Optional — e.g. UPI / bank reference. Must be unique."
          />
          <FormTextarea className="full" label="Notes" name="notes" value={values.notes} onChange={onChange} />
        </div>
      </form>
    </Modal>
  );
}
