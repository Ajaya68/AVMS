import api from "./api";

/**
 * Authentication service.
 *
 * Wired to the backend in the Phase 2 authentication work. The function
 * signatures below are the intended contract so the rest of the UI can
 * already depend on them.
 */

export async function login(email, password) {
  const response = await api.post("/auth/login/", { email, password });
  return response; // { access, refresh, user }
}

export async function refreshToken(refresh) {
  return api.post("/auth/refresh/", { refresh });
}

export async function logout() {
  try {
    await api.post("/auth/logout/");
  } finally {
    localStorage.removeItem("access_token");
    localStorage.removeItem("refresh_token");
  }
}

export default { login, refreshToken, logout };