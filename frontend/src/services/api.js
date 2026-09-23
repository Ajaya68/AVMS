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
  const ventureId = localStorage.getItem("active_venture_id");
  if (ventureId) {
    config.headers["X-Venture-Id"] = ventureId;
  }
  return config;
});

export function setActiveVentureId(ventureId) {
  if (ventureId) {
    localStorage.setItem("active_venture_id", String(ventureId));
  } else {
    localStorage.removeItem("active_venture_id");
  }
}

export function getActiveVentureId() {
  return localStorage.getItem("active_venture_id") || "";
}

// --- Refresh flow ---------------------------------------------------------
// A single-flight refresh: when several requests fail with 401 at the same
// time, only one /auth/refresh/ call is issued and the others wait on it.

let refreshPromise = null;

function refreshAccessToken() {
  const refresh = localStorage.getItem("refresh_token");
  if (!refresh) {
    return Promise.reject(new Error("No refresh token available"));
  }
  if (!refreshPromise) {
    refreshPromise = axios
      .post(`${API_URL}/auth/refresh/`, { refresh })
      .then((res) => {
        const { access, refresh: newRefresh } = res.data;
        localStorage.setItem("access_token", access);
        if (newRefresh) {
          localStorage.setItem("refresh_token", newRefresh);
        }
        return access;
      })
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
}

function clearSession() {
  localStorage.removeItem("access_token");
  localStorage.removeItem("refresh_token");
}

// Normalize responses. The backend envelopes payloads as
// { success, data, message } for success and { success, message, errors }
// for failures. SimpleJWT's raw refresh response is left untouched.
api.interceptors.response.use(
  (response) => {
    const body = response.data;
    if (body && typeof body === "object" && "success" in body) {
      return body.data;
    }
    return response.data;
  },
  async (error) => {
    const original = error.config;
    const isAuthUrl = original?.url?.includes("/auth/");
    if (
      error.response?.status === 401 &&
      !isAuthUrl &&
      original &&
      !original._retried
    ) {
      original._retried = true;
      try {
        await refreshAccessToken();
        return api(original);
      } catch (refreshError) {
        clearSession();
        if (window.location.pathname !== "/login") {
          window.location.assign("/login");
        }
        return Promise.reject(refreshError);
      }
    }
    return Promise.reject(error);
  }
);

export { clearSession };
export default api;