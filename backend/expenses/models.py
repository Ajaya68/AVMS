from decimal import Decimal

from django.conf import settings
from django.db import models

from core.models import TimeStampedModel
from ventures.models import Venture


class ExpenseCategory(models.Model):
    category_code = models.CharField(max_length=30, unique=True)
    category_name = models.CharField(max_length=100)

    class Meta:
        ordering = ["category_name"]

    def __str__(self):
        return self.category_name


class Expense(TimeStampedModel):
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

    venture = models.ForeignKey(Venture, related_name="expenses", on_delete=models.CASCADE)
    category = models.ForeignKey(
        ExpenseCategory, related_name="expenses", on_delete=models.PROTECT
    )
    amount = models.DecimalField(max_digits=14, decimal_places=2)
    expense_date = models.DateField()
    payment_method = models.CharField(max_length=15, choices=METHOD_CHOICES, default=METHOD_CASH)
    description = models.CharField(max_length=255, blank=True, default="")
    created_by = models.ForeignKey(
        settings.AUTH_USER_MODEL, null=True, blank=True, related_name="+",
        on_delete=models.SET_NULL,
    )

    class Meta:
        ordering = ["-expense_date", "-id"]

    def __str__(self):
        return f"{self.category} {self.amount}"

    def save(self, *args, **kwargs):
        self.amount = Decimal(str(self.amount or 0)).quantize(Decimal("0.01"))
        super().save(*args, **kwargs)