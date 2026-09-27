import api from "./api";

export async function fetchUsers(params = {}) {
  return api.get("/auth/users/", { params });
}

export async function createUser(payload) {
  return api.post("/auth/users/", payload);
}

export async function updateUser(id, payload) {
  return api.patch(`/auth/users/${id}/`, payload);
}

export async function deleteUser(id) {
  return api.delete(`/auth/users/${id}/`);
}

export async function fetchRoles() {
  return api.get("/auth/roles/");
}

export default { fetchUsers, createUser, updateUser, deleteUser, fetchRoles };