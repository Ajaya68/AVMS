import axios from "axios";

const API_URL = import.meta.env.VITE_API_URL || "http://localhost:8000/api";

const api = axios.create({
  baseURL: API_URL,
  timeout: 30000,
  headers: {
    "Content-Type": "application/json",
  },
});

// Attach the JWT access token when present.
api.interceptors.request.use((config) => {
  const token = localStorage.getItem("access_token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Normalize responses. The backend envelopes payloads as
// { success, data, message } for success and { success, message, errors }
// for failures.
api.interceptors.response.use(
  (response) => {
    const body = response.data;
    if (body && typeof body === "object" && "success" in body) {
      return body.data;
    }
    return response.data;
  },
  (error) => {
    if (error.response?.status === 401) {
      // Refresh handling is wired in during the authentication phase.
      // For now, clear stale credentials so the UI can redirect to login.
      localStorage.removeItem("access_token");
      localStorage.removeItem("refresh_token");
    }
    return Promise.reject(error);
  }
);

export default api;