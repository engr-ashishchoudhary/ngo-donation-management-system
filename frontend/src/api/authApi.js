import api from './axiosInstance';

export const adminLogin = (payload) => api.post('/api/auth/admin/login', payload).then((r) => r.data);
export const donorLogin = (payload) => api.post('/api/auth/donor/login', payload).then((r) => r.data);
export const donorRegister = (payload) => api.post('/api/auth/donor/register', payload).then((r) => r.data);
// Requires an authenticated SUPER_ADMIN
export const adminRegister = (payload) => api.post('/api/auth/admin/register', payload).then((r) => r.data);
