import api from "./api";

/** Fetches the backend health status from /api/health/. */
export async function getHealth() {
  return api.get("/health/");
}

export default { getHealth };