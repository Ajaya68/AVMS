from decimal import Decimal

from django.conf import settings
from django.db import models

from core.models import TimeStampedModel
from ventures.models import Venture


def money2(value) -> Decimal:
    return Decimal(str(value or 0)).quantize(Decimal("0.01"))


class Payment(TimeStampedModel):
    TYPE_RECEIVED = "RECEIVED"
    TYPE_PAID = "PAID"
    TYPE_CHOICES = [
        (TYPE_RECEIVED, "Received"),
        (TYPE_PAID, "Paid"),
    ]

    REF_SALE = "SALE"
    REF_PURCHASE = "PURCHASE"
    REF_CHOICES = [
        (REF_SALE, "Sale"),
        (REF_PURCHASE, "Purchase"),
    ]

    METHOD_CASH = "CASH"
    METHOD_UPI = "UPI"
    METHOD_BANK = "BANK_TRANSFER"
    METHOD_CARD = "CARD"
    METHOD_OTHER = "OTHER"
    METHOD_CHOICES = [
        (METHOD_CASH, "Cash"),
        (METHOD_UPI, "UPI"),
        (METHOD_BANK, "Bank transfer"),
        (METHOD_CARD, "Card"),
        (METHOD_OTHER, "Other"),
    ]

    venture = models.ForeignKey(Venture, related_name="payments", on_delete=models.CASCADE)
    payment_type = models.CharField(max_length=10, choices=TYPE_CHOICES)
    reference_type = models.CharField(max_length=10, choices=REF_CHOICES)
    reference_id = models.PositiveIntegerField()
    amount = models.DecimalField(max_digits=14, decimal_places=2)
    payment_date = models.DateField()
    payment_method = models.CharField(max_length=15, choices=METHOD_CHOICES, default=METHOD_CASH)
    transaction_reference = models.CharField(max_length=100, blank=True, default="")
    notes = models.CharField(max_length=255, blank=True, default="")
    created_by = models.ForeignKey(
        settings.AUTH_USER_MODEL, null=True, blank=True, related_name="+",
        on_delete=models.SET_NULL,
    )

    class Meta:
        ordering = ["-payment_date", "-id"]

    def __str__(self):
        return f"{self.payment_type} {self.amount} on {self.reference_type} {self.reference_id}"