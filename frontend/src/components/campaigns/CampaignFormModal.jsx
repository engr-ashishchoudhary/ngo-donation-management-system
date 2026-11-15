import { useEffect, useState } from 'react';
import Modal from '../common/Modal';
import Alert from '../common/Alert';
import LoadingSpinner from '../common/LoadingSpinner';
import FormInput from '../forms/FormInput';
import FormSelect from '../forms/FormSelect';
import FormTextarea from '../forms/FormTextarea';
import { createCampaign, updateCampaign } from '../../api/campaignApi';
import { getErrorMessage } from '../../utils/errors';
import { toInputDate } from '../../utils/format';
import { useToast } from '../../context/ToastContext';

const empty = { name: '', description: '', goalAmount: '', startDate: '', endDate: '', status: 'ACTIVE' };

// Create when `campaign` is null, otherwise edit. Mirrors CampaignRequest validation.
export default function CampaignFormModal({ open, campaign, onClose, onSaved }) {
  const toast = useToast();
  const isEdit = Boolean(campaign);
  const [values, setValues] = useState(empty);
  const [errors, setErrors] = useState({});
  const [apiError, setApiError] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!open) return;
    setErrors({});
    setApiError('');
    setValues(
      campaign
        ? {
            name: campaign.name || '',
            // The campaign endpoints do not return the description, so it starts empty.
            description: campaign.description || '',
            goalAmount: campaign.goalAmount ?? '',
            startDate: toInputDate(campaign.startDate),
            endDate: toInputDate(campaign.endDate),
            status: campaign.status || 'ACTIVE',
          }
        : empty
    );
  }, [open, campaign]);

  const onChange = (e) => {
    setValues((v) => ({ ...v, [e.target.name]: e.target.value }));
    setErrors((er) => ({ ...er, [e.target.name]: undefined }));
  };

  const validate = () => {
    const next = {};
    if (!values.name.trim()) next.name = 'Campaign name is required';
    else if (values.name.trim().length > 200) next.name = 'Campaign name must not exceed 200 characters';
    if (values.goalAmount === '' || Number(values.goalAmount) < 0.01) next.goalAmount = 'Goal amount must be greater than 0';
    if (!values.startDate) next.startDate = 'Start date is required';
    if (!values.endDate) next.endDate = 'End date is required';
    if (values.startDate && values.endDate && values.endDate <= values.startDate)
      next.endDate = 'End date must be strictly after start date';
    setErrors(next);
    return Object.keys(next).length === 0;
  };

  const onSubmit = async (e) => {
    e.preventDefault();
    setApiError('');
    if (!validate()) return;
    const payload = {
      name: values.name.trim(),
      description: values.description.trim() || null,
      goalAmount: Number(values.goalAmount),
      startDate: values.startDate,
      endDate: values.endDate,
    };
    setSaving(true);
    try {
      if (isEdit) {
        await updateCampaign(campaign.campaignId, { ...payload, status: values.status });
        toast.success('Campaign updated.');
      } else {
        await createCampaign(payload);
        toast.success('Campaign created.');
      }
      onSaved();
    } catch (err) {
      setApiError(getErrorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  // ACTIVE campaigns can only move to COMPLETED here; closing has its own action.
  const statusOptions = [
    { value: 'ACTIVE', label: 'Active' },
    { value: 'COMPLETED', label: 'Completed' },
  ];

  return (
    <Modal
      open={open}
      title={isEdit ? 'Edit campaign' : 'Create campaign'}
      onClose={onClose}
      closeDisabled={saving}
      size="lg"
      footer={
        <>
          <button type="button" className="btn btn-secondary" onClick={onClose} disabled={saving}>
            Cancel
          </button>
          <button type="submit" form="campaign-form" className="btn btn-primary" disabled={saving}>
            {saving && <LoadingSpinner small />}
            {isEdit ? 'Save changes' : 'Create campaign'}
          </button>
        </>
      }
    >
      <form id="campaign-form" onSubmit={onSubmit} noValidate className="stack" style={{ gap: 16 }}>
        <Alert type="error">{apiError}</Alert>
        {isEdit && (
          <Alert type="warning">
            The campaign API does not return the saved description. Re-enter it below to keep one — saving with this
            field empty clears it.
          </Alert>
        )}
        <div className="form-grid">
          <FormInput className="full" label="Campaign name" name="name" value={values.name} onChange={onChange} error={errors.name} required />
          <FormTextarea className="full" label="Description" name="description" value={values.description} onChange={onChange} error={errors.description} />
          <FormInput
            label="Goal amount (₹)"
            name="goalAmount"
            type="number"
            min="0.01"
            step="0.01"
            value={values.goalAmount}
            onChange={onChange}
            error={errors.goalAmount}
            required
          />
          {isEdit ? (
            <FormSelect label="Status" name="status" value={values.status} onChange={onChange} options={statusOptions} />
          ) : (
            <div />
          )}
          <FormInput label="Start date" name="startDate" type="date" value={values.startDate} onChange={onChange} error={errors.startDate} required />
          <FormInput label="End date" name="endDate" type="date" value={values.endDate} onChange={onChange} error={errors.endDate} required />
        </div>
      </form>
    </Modal>
  );
}
