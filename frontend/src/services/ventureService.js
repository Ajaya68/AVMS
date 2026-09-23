import api from "./api";

export const VENTURE_STATUSES = ["ACTIVE", "INACTIVE"];

export const VENTURE_BUSINESS_TYPES = [
  "MUSHROOM",
  "FISH_FARMING",
  "AGRICULTURE",
  "POULTRY",
  "DAIRY",
  "GENERAL",
  "OTHER",
];

export async function fetchVentures(params = {}) {
  return api.get("/ventures/", { params });
}

export async function fetchVenture(id) {
  return api.get(`/ventures/${id}/`);
}

export async function createVenture(payload) {
  return api.post("/ventures/", payload);
}

export async function updateVenture(id, payload) {
  return api.patch(`/ventures/${id}/`, payload);
}

export async function deleteVenture(id) {
  return api.delete(`/ventures/${id}/`);
}

export default {
  fetchVentures,
  fetchVenture,
  createVenture,
  updateVenture,
  deleteVenture,
  VENTURE_STATUSES,
  VENTURE_BUSINESS_TYPES,
};