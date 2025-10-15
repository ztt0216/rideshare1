import axios from 'axios';

// Vite uses import.meta.env instead of process.env
const API_BASE_URL = import.meta.env.VITE_API_URL || '/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// User APIs
export const register = async (userData) => {
  const response = await api.post('/users', userData);
  return response.data;
};

export const login = async (credentials) => {
  const response = await api.post('/users/login', credentials);
  return response.data;
};

export const getUser = async (userId) => {
  const response = await api.get(`/users/${userId}`);
  return response.data;
};

export const updateWallet = async (userId, amount) => {
  const response = await api.post(`/users/${userId}/wallet`, { amount });
  return response.data;
};

// Ride APIs
export const requestRide = async (rideData) => {
  const response = await api.post('/rides', rideData);
  return response.data;
};

export const getAvailableRides = async () => {
  const response = await api.get('/rides');
  return response.data;
};

export const getRide = async (rideId) => {
  const response = await api.get(`/rides/${rideId}`);
  return response.data;
};

export const acceptRide = async (rideId, driverId) => {
  const response = await api.post(`/rides/${rideId}/accept`, { driverId });
  return response.data;
};

export const startRide = async (rideId, driverId) => {
  const response = await api.post(`/rides/${rideId}/start`, { driverId });
  return response.data;
};

export const completeRide = async (rideId, driverId) => {
  const response = await api.post(`/rides/${rideId}/complete`, { driverId });
  return response.data;
};

export const cancelRide = async (rideId, riderId) => {
  const response = await api.post(`/rides/${rideId}/cancel`, { riderId });
  return response.data;
};

export const getRiderHistory = async (riderId) => {
  const response = await api.get(`/rides/rider/${riderId}`);
  return response.data;
};

export const getDriverHistory = async (driverId) => {
  const response = await api.get(`/rides/driver/${driverId}`);
  return response.data;
};

// Driver Availability APIs
export const setAvailability = async (driverId, schedules) => {
  const response = await api.post(`/drivers/availability/${driverId}`, { schedules });
  return response.data;
};

export const getAvailability = async (driverId) => {
  const response = await api.get(`/drivers/availability/${driverId}`);
  return response.data;
};

export default api;
