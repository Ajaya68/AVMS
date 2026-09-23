from django.urls import path

from .views import (
    InventoryListView,
    StockMovementDetailView,
    StockMovementListCreateView,
    WarehouseDetailView,
    WarehouseListCreateView,
)

urlpatterns = [
    path("warehouses/", WarehouseListCreateView.as_view(), name="warehouse-list"),
    path("warehouses/<int:pk>/", WarehouseDetailView.as_view(), name="warehouse-detail"),
    path("inventory/", InventoryListView.as_view(), name="inventory-list"),
    path("stock-movements/", StockMovementListCreateView.as_view(), name="stock-movement-list"),
    path("stock-movements/<int:pk>/", StockMovementDetailView.as_view(), name="stock-movement-detail"),
]