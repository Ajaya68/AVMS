from django.urls import path

from .views import (
    PurchaseDetailView,
    PurchaseItemsView,
    PurchaseListCreateView,
    PurchaseReturnDetailView,
    PurchaseReturnListCreateView,
)

urlpatterns = [
    path("purchases/", PurchaseListCreateView.as_view(), name="purchase-list"),
    path("purchases/<int:pk>/", PurchaseDetailView.as_view(), name="purchase-detail"),
    path("purchases/<int:pk>/items/", PurchaseItemsView.as_view(), name="purchase-items"),
    path("purchase-returns/", PurchaseReturnListCreateView.as_view(), name="purchase-return-list"),
    path("purchase-returns/<int:pk>/", PurchaseReturnDetailView.as_view(), name="purchase-return-detail"),
]