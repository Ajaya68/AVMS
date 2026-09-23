import api from "./api";

export async function fetchCustomers(params = {}) {
  return api.get("/customers/", { params });
}

export async function createCustomer(payload) {
  return api.post("/customers/", payload);
}

export async function updateCustomer(id, payload) {
  return api.patch(`/customers/${id}/`, payload);
}

export async function deleteCustomer(id) {
  return api.delete(`/customers/${id}/`);
}

export default {
  fetchCustomers,
  createCustomer,
  updateCustomer,
  deleteCustomer,
};