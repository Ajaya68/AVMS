"""Django admin registration for the audit log."""

from django.contrib import admin

from .models import AuditLog


@admin.register(AuditLog)
class AuditLogAdmin(admin.ModelAdmin):
    list_display = [
        "id",
        "user",
        "action",
        "module",
        "object_type",
        "object_id",
        "ip_address",
        "created_at",
    ]
    list_filter = ["action", "module", "created_at"]
    search_fields = ["user__email", "description", "object_type", "object_id"]
    readonly_fields = [f.name for f in AuditLog._meta.fields]
    date_hierarchy = "created_at"

    def has_add_permission(self, request):
        return False

    def has_change_permission(self, request, obj=None):
        return False

    def has_delete_permission(self, request, obj=None):
        return False