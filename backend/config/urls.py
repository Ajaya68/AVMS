"""
AVMS backend - root URL configuration.

API endpoints are mounted under /api/. Every feature app contributes its
own URL module which is wired in below as it is implemented.
"""

from django.contrib import admin
from django.urls import include, path
from drf_spectacular.views import SpectacularAPIView, SpectacularSwaggerView

from core.views import HealthView

urlpatterns = [
    path("admin/", admin.site.urls),
    # Health checks (no auth required)
    path("api/health/", HealthView.as_view(), name="health"),
    path("health/", HealthView.as_view(), name="health-short"),
    # OpenAPI documentation
    path("api/schema/", SpectacularAPIView.as_view(), name="schema"),
    path(
        "api/docs/",
        SpectacularSwaggerView.as_view(url_name="schema"),
        name="swagger-ui",
    ),
    # Feature apps (wired in phase by phase)
    path("api/auth/", include("accounts.urls")),
    path("api/", include("audit.urls")),
    path("api/notifications/", include("notifications.urls")),
    path("api/ventures/", include("ventures.urls")),
    path("api/customers/", include("customers.urls")),
    path("api/suppliers/", include("suppliers.urls")),
    path("api/", include("products.urls")),
    path("api/", include("inventory.urls")),
    path("api/", include("purchases.urls")),
    path("api/", include("sales.urls")),
    path("api/", include("payments.urls")),
    path("api/", include("expenses.urls")),
    path("api/", include("employees.urls")),
    path("api/reports/", include("reports.urls")),
]