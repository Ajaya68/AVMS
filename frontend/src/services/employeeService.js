import api from "./api";

export const fetchEmployees = (params = {}) =>
  api.get("/employees/", { params: { page_size: 100, ...params } }).then((r) => r.data.data);

export const fetchEmployee = (id) => api.get(`/employees/${id}/`).then((r) => r.data.data);

export const createEmployee = (payload) =>
  api.post("/employees/", payload).then((r) => r.data.data);

export const updateEmployee = (id, payload) =>
  api.patch(`/employees/${id}/`, payload).then((r) => r.data.data);

export const deleteEmployee = (id) => api.delete(`/employees/${id}/`).then((r) => r.data);