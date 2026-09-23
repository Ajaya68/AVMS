from decimal import Decimal, ROUND_HALF_UP

from django.conf import settings
from django.db import models

from core.models import TimeStampedModel
from core.services import generate_code

from products.models import Product
from suppliers.models import Supplier
from ventures.models import Venture


def money2(value) -> Decimal:
    return Decimal(str(value or 0)).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)


class Purchase(TimeStampedModel):
    STATUS_PENDING = "PENDING"
    STATUS_PARTIAL = "PARTIAL"
    STATUS_COMPLETED = "COMPLETED"
    STATUS_RETURNED = "RETURNED"
    STATUS_CANCELLED = "CANCELLED"
    STATUS_CHOICES = [
        (STATUS_PENDING, "Pending"),
        (STATUS_PARTIAL, "Partially returned"),
        (STATUS_COMPLETED, "Completed"),
        (STATUS_RETURNED, "Returned"),
        (STATUS_CANCELLED, "Cancelled"),
    ]

    venture = models.ForeignKey(Venture, related_name="purchases", on_delete=models.CASCADE)
    supplier = models.ForeignKey(Supplier, related_name="purchases", on_delete=models.PROTECT)
    warehouse = models.ForeignKey(
        "inventory.Warehouse", related_name="purchases", null=True, blank=True,
        on_delete=models.SET_NULL,
    )
    invoice_number = models.CharField(max_length=20, editable=False)
    purchase_date = models.DateField()
    status = models.CharField(max_length=12, choices=STATUS_CHOICES, default=STATUS_COMPLETED)
    subtotal = models.DecimalField(max_digits=14, decimal_places=2, default=0)
    discount = models.DecimalField(max_digits=14, decimal_places=2, default=0)
    tax = models.DecimalField(max_digits=14, decimal_places=2, default=0)
    total_amount = models.DecimalField(max_digits=14, decimal_places=2, default=0)
    paid_amount = models.DecimalField(max_digits=14, decimal_places=2, default=0)
    returned_amount = models.DecimalField(max_digits=14, decimal_places=2, default=0)
    due_amount = models.DecimalField(max_digits=14, decimal_places=2, default=0)
    notes = models.CharField(max_length=255, blank=True, default="")
    created_by = models.ForeignKey(
        settings.AUTH_USER_MODEL, null=True, blank=True, related_name="+",
        on_delete=models.SET_NULL,
    )

    class Meta:
        ordering = ["-purchase_date", "-id"]
        constraints = [
            models.UniqueConstraint(
                fields=["venture", "invoice_number"], name="uniq_invoice_per_venture"
            )
        ]

    def __str__(self):
        return f"PO {self.invoice_number} - {self.supplier}"

    def save(self, *args, **kwargs):
        if not self.invoice_number:
            self.invoice_number = generate_code("purchases", self.venture_id, "PINV")
        self.recompute()
        super().save(*args, **kwargs)

    def recompute(self):
        """Recalculate bill totals from item lines."""
        if not self.pk:
            return
        subtotal = sum(
            (money2(item.quantity) * money2(item.unit_price))
            for item in self.items.all()
        )
        self.subtotal = money2(subtotal)
        self.total_amount = money2(self.subtotal - self.discount + self.tax)
        self.due_amount = money2(self.total_amount - self.paid_amount - self.returned_amount)
        if self.due_amount < 0:
            self.due_amount = Decimal("0")


class PurchaseItem(models.Model):
    purchase = models.ForeignKey(Purchase, related_name="items", on_delete=models.CASCADE)
    product = models.ForeignKey(Product, related_name="purchase_items", on_delete=models.PROTECT)
    quantity = models.DecimalField(max_digits=14, decimal_places=2)
    unit_price = models.DecimalField(max_digits=14, decimal_places=2)
    discount = models.DecimalField(max_digits=14, decimal_places=2, default=0)
    tax = models.DecimalField(max_digits=14, decimal_places=2, default=0)
    total = models.DecimalField(max_digits=14, decimal_places=2, default=0)

    class Meta:
        ordering = ["id"]

    def recompute(self):
        base = money2(self.quantity) * money2(self.unit_price)
        discounted = money2(base - self.discount)
        self.total = money2(discounted + discounted * money2(self.tax) / Decimal("100"))
        return self.total

    def __str__(self):
        return f"{self.product} x {self.quantity}"


class PurchaseReturn(TimeStampedModel):
    STATUS_PENDING = "PENDING"
    STATUS_COMPLETED = "COMPLETED"
    STATUS_CHOICES = [
        (STATUS_PENDING, "Pending"),
        (STATUS_COMPLETED, "Completed"),
    ]

    venture = models.ForeignKey(Venture, related_name="purchase_returns", on_delete=models.CASCADE)
    purchase = models.ForeignKey(Purchase, related_name="returns", on_delete=models.PROTECT)
    return_number = models.CharField(max_length=20, editable=False)
    return_date = models.DateField()
    status = models.CharField(max_length=12, choices=STATUS_CHOICES, default=STATUS_COMPLETED)
    total_amount = models.DecimalField(max_digits=14, decimal_places=2, default=0)
    notes = models.CharField(max_length=255, blank=True, default="")
    created_by = models.ForeignKey(
        settings.AUTH_USER_MODEL, null=True, blank=True, related_name="+",
        on_delete=models.SET_NULL,
    )

    class Meta:
        ordering = ["-return_date", "-id"]
        constraints = [
            models.UniqueConstraint(
                fields=["venture", "return_number"], name="uniq_return_per_venture"
            )
        ]

    def __str__(self):
        return f"RET {self.return_number}"

    def save(self, *args, **kwargs):
        if not self.return_number:
            self.return_number = generate_code("purchase_returns", self.venture_id, "RET")
        if not self.pk:
            self.total_amount = Decimal("0")
        super().save(*args, **kwargs)


class PurchaseReturnItem(models.Model):
    purchase_return = models.ForeignKey(
        PurchaseReturn, related_name="items", on_delete=models.CASCADE
    )
    product = models.ForeignKey(Product, related_name="purchase_return_items", on_delete=models.PROTECT)
    quantity = models.DecimalField(max_digits=14, decimal_places=2)
    unit_price = models.DecimalField(max_digits=14, decimal_places=2)
    total = models.DecimalField(max_digits=14, decimal_places=2, default=0)

    class Meta:
        ordering = ["id"]

    def recompute(self):
        self.total = money2(money2(self.quantity) * money2(self.unit_price))
        return self.total