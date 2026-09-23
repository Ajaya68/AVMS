import api from "./api";

export const fetchEmployees = (params = {}) =>
  api.get("/employees/", { params: { page_size: 100, ...params } });

export const fetchEmployee = (id) => api.get(`/employees/${id}/`);

export const createEmployee = (payload) =>
  api.post("/employees/", payload);

export const updateEmployee = (id, payload) =>
  api.patch(`/employees/${id}/`, payload);

export const deleteEmployee = (id) => api.delete(`/employees/${id}/`).then((r) => r.data);