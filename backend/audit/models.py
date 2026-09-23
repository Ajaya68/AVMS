"""Audit log model.

Phase 2 introduces the audit log with authentication events (LOGIN/LOGOUT)
and user administration. The full audit surface (business module events)
arrives in Phase 11; the storage model is intentionally complete so nothing
needs to change later.
"""

from django.conf import settings
from django.db import models


class AuditLog(models.Model):
    ACTION_LOGIN = "LOGIN"
    ACTION_LOGOUT = "LOGOUT"
    ACTION_CREATE = "CREATE"
    ACTION_UPDATE = "UPDATE"
    ACTION_DELETE = "DELETE"
    ACTION_SALE_CREATED = "SALE_CREATED"
    ACTION_PURCHASE_CREATED = "PURCHASE_CREATED"
    ACTION_PAYMENT_CREATED = "PAYMENT_CREATED"
    ACTION_STOCK_ADJUSTED = "STOCK_ADJUSTED"
    ACTION_USER_CREATED = "USER_CREATED"

    ACTION_CHOICES = [
        (ACTION_LOGIN, "Login"),
        (ACTION_LOGOUT, "Logout"),
        (ACTION_CREATE, "Create"),
        (ACTION_UPDATE, "Update"),
        (ACTION_DELETE, "Delete"),
        (ACTION_SALE_CREATED, "Sale created"),
        (ACTION_PURCHASE_CREATED, "Purchase created"),
        (ACTION_PAYMENT_CREATED, "Payment created"),
        (ACTION_STOCK_ADJUSTED, "Stock adjusted"),
        (ACTION_USER_CREATED, "User created"),
    ]

    user = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        null=True,
        blank=True,
        on_delete=models.SET_NULL,
        related_name="audit_logs",
    )
    action = models.CharField(max_length=40, choices=ACTION_CHOICES)
    module = models.CharField(max_length=50, db_index=True)
    object_type = models.CharField(max_length=80, blank=True, default="")
    object_id = models.CharField(max_length=40, blank=True, default="")
    ip_address = models.GenericIPAddressField(null=True, blank=True)
    description = models.TextField(blank=True, default="")
    created_at = models.DateTimeField(auto_now_add=True, db_index=True)

    class Meta:
        ordering = ["-created_at"]
        verbose_name = "Audit Log"
        verbose_name_plural = "Audit Logs"

    def __str__(self):
        return f"{self.user_id} {self.action} {self.module} @ {self.created_at}"