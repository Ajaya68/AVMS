"""Django admin registration for ventures."""

from django.contrib import admin

from .models import Venture


@admin.register(Venture)
class VentureAdmin(admin.ModelAdmin):
    list_display = [
        "venture_code",
        "venture_name",
        "business_type",
        "city",
        "status",
        "created_at",
    ]
    list_filter = ["status", "business_type"]
    search_fields = ["venture_code", "venture_name", "city", "state"]
    readonly_fields = ["venture_code"]