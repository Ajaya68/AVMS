from django.db import models

from core.models import TimeStampedModel
from core.services import generate_code

from ventures.models import Venture


class Unit(models.Model):
    """Static reference of measurement units (kg, g, pcs, ...)."""

    unit_code = models.CharField(max_length=20, unique=True)
    unit_name = models.CharField(max_length=50)
    is_base = models.BooleanField(default=True, help_text="Base unit for conversions")

    class Meta:
        ordering = ["unit_code"]

    def __str__(self):
        return self.unit_code


class Category(TimeStampedModel):
    STATUS_ACTIVE = "ACTIVE"
    STATUS_INACTIVE = "INACTIVE"
    STATUS_CHOICES = [
        (STATUS_ACTIVE, "Active"),
        (STATUS_INACTIVE, "Inactive"),
    ]

    venture = models.ForeignKey(Venture, related_name="categories", on_delete=models.CASCADE)
    category_name = models.CharField(max_length=100)
    description = models.CharField(max_length=255, blank=True, default="")
    status = models.CharField(max_length=10, choices=STATUS_CHOICES, default=STATUS_ACTIVE)

    class Meta:
        ordering = ["category_name"]
        constraints = [
            models.UniqueConstraint(
                fields=["venture", "category_name"], name="uniq_category_per_venture"
            )
        ]

    def __str__(self):
        return self.category_name


class Product(TimeStampedModel):
    STATUS_ACTIVE = "ACTIVE"
    STATUS_INACTIVE = "INACTIVE"
    STATUS_CHOICES = [
        (STATUS_ACTIVE, "Active"),
        (STATUS_INACTIVE, "Inactive"),
    ]

    venture = models.ForeignKey(Venture, related_name="products", on_delete=models.CASCADE)
    category = models.ForeignKey(Category, related_name="products", null=True, blank=True, on_delete=models.SET_NULL)
    unit = models.ForeignKey(Unit, related_name="products", on_delete=models.PROTECT)
    sku = models.CharField(max_length=20, editable=False)
    product_name = models.CharField(max_length=150)
    description = models.CharField(max_length=255, blank=True, default="")
    purchase_price = models.DecimalField(max_digits=12, decimal_places=2, default=0)
    selling_price = models.DecimalField(max_digits=12, decimal_places=2, default=0)
    tax_rate = models.DecimalField(max_digits=5, decimal_places=2, default=0)
    reorder_level = models.DecimalField(max_digits=12, decimal_places=2, default=0)
    status = models.CharField(max_length=10, choices=STATUS_CHOICES, default=STATUS_ACTIVE)

    class Meta:
        ordering = ["product_name"]
        constraints = [
            models.UniqueConstraint(fields=["venture", "sku"], name="uniq_sku_per_venture"),
        ]

    def __str__(self):
        return f"{self.sku} - {self.product_name}"

    def save(self, *args, **kwargs):
        if not self.sku:
            self.sku = generate_code("products", self.venture_id, "P")
        super().save(*args, **kwargs)