import api from './axiosInstance';

export const getDonations = ({ campaignId, status } = {}) =>
  api
    .get('/api/donations', {
      params: {
        ...(campaignId ? { campaignId } : {}),
        ...(status ? { status } : {}),
      },
    })
    .then((r) => r.data);
export const getMyDonations = () => api.get('/api/donations/my').then((r) => r.data);
export const getDonation = (id) => api.get(`/api/donations/${id}`).then((r) => r.data);
export const createDonation = (payload) => api.post('/api/donations', payload).then((r) => r.data);
export const updateDonationStatus = (id, status) =>
  api.patch(`/api/donations/${id}/status`, { status }).then((r) => r.data);
