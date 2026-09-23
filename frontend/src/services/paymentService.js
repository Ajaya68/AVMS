import api from "./api";

export async function fetchPayments(params = {}) {
  return api.get("/payments/", { params });
}

export async function createPayment(payload) {
  return api.post("/payments/", payload);
}

export async function deletePayment(id) {
  return api.delete(`/payments/${id}/`);
}

export const PAYMENT_TYPES = ["RECEIVED", "PAID"];

export const PAYMENT_METHODS = [
  "CASH",
  "UPI",
  "BANK_TRANSFER",
  "CARD",
  "OTHER",
];

export function label(value) {
  return String(value || "")
    .replace(/_/g, " ")
    .replace(/\b\w/g, (c) => c.toUpperCase());
}

export function typeBadge(type) {
  return type === "RECEIVED" ? "success" : "danger";
}

export default {
  fetchPayments,
  createPayment,
  deletePayment,
  PAYMENT_TYPES,
  PAYMENT_METHODS,
  label,
  typeBadge,
};