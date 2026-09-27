import api, { clearSession } from "./api";

/**
 * Authentication service.
 *
 * All token storage lives here; the rest of the app talks to these
 * functions and to the AuthContext.
 */

const ACCESS_KEY = "access_token";
const REFRESH_KEY = "refresh_token";

export function getAccessToken() {
  return localStorage.getItem(ACCESS_KEY);
}

export function getRefreshToken() {
  return localStorage.getItem(REFRESH_KEY);
}

export function setTokens(access, refresh) {
  localStorage.setItem(ACCESS_KEY, access);
  if (refresh) {
    localStorage.setItem(REFRESH_KEY, refresh);
  }
}

export async function login(email, password) {
  const data = await api.post("/auth/login/", { email, password });
  setTokens(data.access, data.refresh);
  return data.user;
}

export async function requestPasswordReset(email) {
  return api.post("/auth/forgot-password/", { email });
}

export async function resetPassword(uidb64, token, newPassword) {
  return api.post("/auth/reset-password/", {
    uidb64,
    token,
    new_password: newPassword,
  });
}

export async function changePassword(currentPassword, newPassword) {
  return api.post("/auth/change-password/", {
    current_password: currentPassword,
    new_password: newPassword,
  });
}

export async function logout() {
  const refresh = getRefreshToken();
  try {
    if (refresh) {
      await api.post("/auth/logout/", { refresh });
    }
  } catch {
    // Token may already be invalid; local logout still proceeds.
  } finally {
    clearSession();
  }
}

export async function fetchMe() {
  return api.get("/auth/me/");
}

export default {
  login,
  logout,
  fetchMe,
  requestPasswordReset,
  resetPassword,
  changePassword,
  getAccessToken,
  getRefreshToken,
  setTokens,
};