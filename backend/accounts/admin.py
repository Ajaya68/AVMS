"""Django admin registration for AVMS users, roles and permissions."""

from django.contrib import admin
from django.contrib.auth.admin import UserAdmin as BaseUserAdmin
from django.utils.translation import gettext_lazy as _

from .models import Permission, Role, User


class UserRoleInline(admin.TabularInline):
    model = User.roles.through
    extra = 0
    verbose_name = "Role"
    verbose_name_plural = "Roles"


@admin.register(User)
class UserAdmin(BaseUserAdmin):
    ordering = ["email"]
    list_display = ["email", "full_name", "phone", "is_active", "is_staff"]
    list_filter = ["is_active", "is_staff", "is_superuser", "roles"]
    search_fields = ["email", "full_name", "phone"]

    fieldsets = (
        (None, {"fields": ("email", "password")}),
        (_("Personal info"), {"fields": ("full_name", "phone")}),
        (
            _("Permissions"),
            {
                "fields": (
                    "is_active",
                    "is_staff",
                    "is_superuser",
                    "groups",
                    "user_permissions",
                )
            },
        ),
        (_("Important dates"), {"fields": ("last_login", "created_at", "updated_at")}),
    )
    readonly_fields = ["created_at", "updated_at"]
    add_fieldsets = (
        (
            None,
            {
                "classes": ("wide",),
                "fields": ("email", "password1", "password2"),
            },
        ),
    )
    inlines = [UserRoleInline]

    def get_queryset(self, request):
        return super().get_queryset(request).prefetch_related("roles")


@admin.register(Role)
class RoleAdmin(admin.ModelAdmin):
    list_display = ["code", "name", "is_active", "created_at"]
    list_filter = ["is_active"]
    search_fields = ["code", "name", "description"]
    filter_horizontal = ["permissions"]


@admin.register(Permission)
class PermissionAdmin(admin.ModelAdmin):
    list_display = ["code", "name", "module"]
    list_filter = ["module"]
    search_fields = ["code", "name"]
    ordering = ["module", "code"]