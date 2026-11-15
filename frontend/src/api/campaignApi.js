import api from './axiosInstance';

export const getCampaigns = (status) =>
  api.get('/api/campaigns', { params: status ? { status } : undefined }).then((r) => r.data);
export const getCampaign = (id) => api.get(`/api/campaigns/${id}`).then((r) => r.data);
export const createCampaign = (payload) => api.post('/api/campaigns', payload).then((r) => r.data);
export const updateCampaign = (id, payload) => api.put(`/api/campaigns/${id}`, payload).then((r) => r.data);
export const closeCampaign = (id) => api.patch(`/api/campaigns/${id}/close`).then((r) => r.data);
