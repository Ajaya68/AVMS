from django.urls import path

from .views import (
    CategoryDetailView,
    CategoryListCreateView,
    ProductDetailView,
    ProductListCreateView,
    UnitDetailView,
    UnitListCreateView,
)

urlpatterns = [
    path("products/", ProductListCreateView.as_view(), name="product-list"),
    path("products/<int:pk>/", ProductDetailView.as_view(), name="product-detail"),
    path("categories/", CategoryListCreateView.as_view(), name="category-list"),
    path("categories/<int:pk>/", CategoryDetailView.as_view(), name="category-detail"),
    path("units/", UnitListCreateView.as_view(), name="unit-list"),
    path("units/<int:pk>/", UnitDetailView.as_view(), name="unit-detail"),
]