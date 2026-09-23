import api from "./api";

export async function fetchSuppliers(params = {}) {
  return api.get("/suppliers/", { params });
}

export async function createSupplier(payload) {
  return api.post("/suppliers/", payload);
}

export async function updateSupplier(id, payload) {
  return api.patch(`/suppliers/${id}/`, payload);
}

export async function deleteSupplier(id) {
  return api.delete(`/suppliers/${id}/`);
}

export default {
  fetchSuppliers,
  createSupplier,
  updateSupplier,
  deleteSupplier,
};