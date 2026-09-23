import api from "./api";

export const fetchNotifications = (params = {}) =>
  api.get("/notifications/", { params: { page_size: 25, ...params } }).then((r) => r.data.data);

export const fetchUnreadCount = () =>
  api.get("/notifications/unread-count/").then((r) => r.data.data);

export const markNotificationRead = (id) =>
  api.post(`/notifications/${id}/read/`).then((r) => r.data.data);

export const markAllNotificationsRead = () =>
  api.post("/notifications/read-all/").then((r) => r.data.data);

export const TYPE_BADGES = {
  LOW_STOCK: "text-bg-danger",
  SALE_CREATED: "text-bg-success",
  PURCHASE_CREATED: "text-bg-info",
  PAYMENT_RECEIVED: "text-bg-success",
  PAYMENT_PAID: "text-bg-warning",
  SYSTEM: "text-bg-secondary",
};

export const TYPE_LABELS = {
  LOW_STOCK: "Low stock",
  SALE_CREATED: "Sale created",
  PURCHASE_CREATED: "Purchase created",
  PAYMENT_RECEIVED: "Payment received",
  PAYMENT_PAID: "Payment made",
  SYSTEM: "System",
};