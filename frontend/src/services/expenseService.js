import api from "./api";

export async function fetchExpenses(params = {}) {
  return api.get("/expenses/", { params });
}

export async function createExpense(payload) {
  return api.post("/expenses/", payload);
}

export async function updateExpense(id, payload) {
  return api.patch(`/expenses/${id}/`, payload);
}

export async function deleteExpense(id) {
  return api.delete(`/expenses/${id}/`);
}

export async function fetchExpenseCategories() {
  return api.get("/expense-categories/");
}

export default {
  fetchExpenses,
  createExpense,
  updateExpense,
  deleteExpense,
  fetchExpenseCategories,
};