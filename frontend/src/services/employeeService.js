import api from "./api";

export const fetchEmployees = (params = {}) =>
  api.get("/employees/", { params: { page_size: 100, ...params } });

export const fetchEmployee = (id) => api.get(`/employees/${id}/`);

export const createEmployee = (payload) =>
  api.post("/employees/", payload);

export const updateEmployee = (id, payload) =>
  api.patch(`/employees/${id}/`, payload);

export const deleteEmployee = (id) => api.delete(`/employees/${id}/`);

export const fetchMyProfile = () => api.get("/employees/me/");

export const fetchAttendance = (params = {}) =>
  api.get("/attendance/", { params });

export const markAttendance = (payload) => api.post("/attendance/", payload);

export const bulkAttendance = (payload) => api.post("/attendance/bulk/", payload);

export const updateAttendance = (id, payload) =>
  api.patch(`/attendance/${id}/`, payload);

export const deleteAttendance = (id) => api.delete(`/attendance/${id}/`);