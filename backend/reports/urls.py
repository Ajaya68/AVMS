from django.urls import path

from .views import (
    FinancialReportView,
    InventoryReportView,
    PurchasesReportView,
    SalesReportView,
)

urlpatterns = [
    path("sales/", SalesReportView.as_view(), name="report-sales"),
    path("purchases/", PurchasesReportView.as_view(), name="report-purchases"),
    path("inventory/", InventoryReportView.as_view(), name="report-inventory"),
    path("financial/", FinancialReportView.as_view(), name="report-financial"),
]