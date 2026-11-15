import api from './axiosInstance';

export const getExpenses = (campaignId) =>
  api.get(`/api/campaigns/${campaignId}/expenses`).then((r) => r.data);
export const createExpense = (campaignId, payload) =>
  api.post(`/api/campaigns/${campaignId}/expenses`, payload).then((r) => r.data);
