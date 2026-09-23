import api from "./api";

export async function fetchProducts(params = {}) {
  return api.get("/products/", { params });
}

export async function fetchProduct(id) {
  return api.get(`/products/${id}/`);
}

export async function createProduct(payload) {
  return api.post("/products/", payload);
}

export async function updateProduct(id, payload) {
  return api.patch(`/products/${id}/`, payload);
}

export async function deleteProduct(id) {
  return api.delete(`/products/${id}/`);
}

export async function fetchCategories(params = {}) {
  return api.get("/categories/", { params });
}

export async function createCategory(payload) {
  return api.post("/categories/", payload);
}

export async function updateCategory(id, payload) {
  return api.patch(`/categories/${id}/`, payload);
}

export async function deleteCategory(id) {
  return api.delete(`/categories/${id}/`);
}

export async function fetchUnits(params = {}) {
  return api.get("/units/", { params });
}

export async function createUnit(payload) {
  return api.post("/units/", payload);
}

export async function updateUnit(id, payload) {
  return api.patch(`/units/${id}/`, payload);
}

export async function deleteUnit(id) {
  return api.delete(`/units/${id}/`);
}

export default {
  fetchProducts,
  fetchProduct,
  createProduct,
  updateProduct,
  deleteProduct,
  fetchCategories,
  createCategory,
  updateCategory,
  deleteCategory,
  fetchUnits,
  createUnit,
  updateUnit,
  deleteUnit,
};