import api from './axiosInstance';

export const getProfile = () => api.get('/api/donor/profile').then((r) => r.data);
export const updateProfile = (payload) => api.put('/api/donor/profile', payload).then((r) => r.data);
