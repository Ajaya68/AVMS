from django.conf import settings
from django.db import models


class Notification(models.Model):
    TYPE_LOW_STOCK = "LOW_STOCK"
    TYPE_SALE_CREATED = "SALE_CREATED"
    TYPE_PURCHASE_CREATED = "PURCHASE_CREATED"
    TYPE_PAYMENT_RECEIVED = "PAYMENT_RECEIVED"
    TYPE_PAYMENT_PAID = "PAYMENT_PAID"
    TYPE_SYSTEM = "SYSTEM"
    TYPE_CHOICES = [
        (TYPE_LOW_STOCK, "Low stock"),
        (TYPE_SALE_CREATED, "Sale created"),
        (TYPE_PURCHASE_CREATED, "Purchase created"),
        (TYPE_PAYMENT_RECEIVED, "Payment received"),
        (TYPE_PAYMENT_PAID, "Payment made"),
        (TYPE_SYSTEM, "System"),
    ]

    user = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        related_name="notifications",
        on_delete=models.CASCADE,
    )
    type = models.CharField(max_length=25, choices=TYPE_CHOICES, default=TYPE_SYSTEM)
    message = models.CharField(max_length=255)
    link = models.CharField(max_length=255, blank=True, default="")
    is_read = models.BooleanField(default=False)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ["-created_at", "-id"]

    def __str__(self):
        return f"{self.user_id} {self.type}: {self.message}"