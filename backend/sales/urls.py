from django.urls import path

from .views import (
    SaleDetailView,
    SaleItemsView,
    SaleListCreateView,
    SaleReturnDetailView,
    SaleReturnListCreateView,
)

urlpatterns = [
    path("sales/", SaleListCreateView.as_view(), name="sale-list"),
    path("sales/<int:pk>/", SaleDetailView.as_view(), name="sale-detail"),
    path("sales/<int:pk>/items/", SaleItemsView.as_view(), name="sale-items"),
    path("sales-returns/", SaleReturnListCreateView.as_view(), name="sale-return-list"),
    path("sales-returns/<int:pk>/", SaleReturnDetailView.as_view(), name="sale-return-detail"),
]