import api from './axiosInstance';

export const getAdmins = () => api.get('/api/admin/admins').then((r) => r.data);
export const getDonors = () => api.get('/api/admin/donors').then((r) => r.data);
export const getDashboard = () => api.get('/api/admin/dashboard').then((r) => r.data);
