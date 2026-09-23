from django.conf import settings
from django.db import models

from core.models import TimeStampedModel
from core.services import generate_code

from products.models import Product
from ventures.models import Venture


class Warehouse(TimeStampedModel):
    STATUS_ACTIVE = "ACTIVE"
    STATUS_INACTIVE = "INACTIVE"
    STATUS_CHOICES = [
        (STATUS_ACTIVE, "Active"),
        (STATUS_INACTIVE, "Inactive"),
    ]

    venture = models.ForeignKey(Venture, related_name="warehouses", on_delete=models.CASCADE)
    warehouse_code = models.CharField(max_length=20, editable=False)
    warehouse_name = models.CharField(max_length=150)
    address = models.CharField(max_length=255, blank=True, default="")
    city = models.CharField(max_length=100, blank=True, default="")
    state = models.CharField(max_length=100, blank=True, default="")
    pincode = models.CharField(max_length=10, blank=True, default="")
    manager = models.CharField(max_length=150, blank=True, default="")
    status = models.CharField(max_length=10, choices=STATUS_CHOICES, default=STATUS_ACTIVE)

    class Meta:
        ordering = ["warehouse_name"]
        constraints = [
            models.UniqueConstraint(
                fields=["venture", "warehouse_code"], name="uniq_warehouse_code_per_venture"
            )
        ]

    def __str__(self):
        return f"{self.warehouse_code} - {self.warehouse_name}"

    def save(self, *args, **kwargs):
        if not self.warehouse_code:
            self.warehouse_code = generate_code("warehouses", self.venture_id, "W")
        super().save(*args, **kwargs)


class Inventory(models.Model):
    """Stock on hand per warehouse/product. Rows are created lazily on the
    first movement and updated transactionally by stock movement services."""

    venture = models.ForeignKey(Venture, related_name="inventory", on_delete=models.CASCADE)
    warehouse = models.ForeignKey(Warehouse, related_name="inventory", on_delete=models.CASCADE)
    product = models.ForeignKey(Product, related_name="inventory", on_delete=models.CASCADE)
    quantity = models.DecimalField(max_digits=14, decimal_places=2, default=0)
    reserved_quantity = models.DecimalField(max_digits=14, decimal_places=2, default=0)
    reorder_level = models.DecimalField(max_digits=14, decimal_places=2, default=0)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        ordering = ["product__product_name"]
        constraints = [
            models.UniqueConstraint(
                fields=["warehouse", "product"], name="uniq_inventory_wh_product"
            )
        ]

    @property
    def available(self):
        return self.quantity - self.reserved_quantity

    @property
    def is_low(self):
        return self.available <= self.reorder_level

    def __str__(self):
        return f"{self.warehouse} / {self.product} = {self.quantity}"


class StockMovement(TimeStampedModel):
    MOVE_PURCHASE = "PURCHASE"
    MOVE_SALE = "SALE"
    MOVE_PURCHASE_RETURN = "PURCHASE_RETURN"
    MOVE_SALES_RETURN = "SALES_RETURN"
    MOVE_ADJUSTMENT_IN = "ADJUSTMENT_IN"
    MOVE_ADJUSTMENT_OUT = "ADJUSTMENT_OUT"
    MOVE_TRANSFER_IN = "TRANSFER_IN"
    MOVE_TRANSFER_OUT = "TRANSFER_OUT"
    MOVEMENT_TYPES = [
        (MOVE_PURCHASE, "Purchase receipt"),
        (MOVE_SALE, "Sale dispatch"),
        (MOVE_PURCHASE_RETURN, "Purchase return"),
        (MOVE_SALES_RETURN, "Sales return"),
        (MOVE_ADJUSTMENT_IN, "Adjustment in"),
        (MOVE_ADJUSTMENT_OUT, "Adjustment out"),
        (MOVE_TRANSFER_IN, "Transfer in"),
        (MOVE_TRANSFER_OUT, "Transfer out"),
    ]

    IN_CREASING = {
        MOVE_PURCHASE,
        MOVE_SALES_RETURN,
        MOVE_ADJUSTMENT_IN,
        MOVE_TRANSFER_IN,
    }
    OUT_DECREASING = {
        MOVE_SALE,
        MOVE_PURCHASE_RETURN,
        MOVE_ADJUSTMENT_OUT,
        MOVE_TRANSFER_OUT,
    }

    venture = models.ForeignKey(Venture, related_name="stock_movements", on_delete=models.CASCADE)
    warehouse = models.ForeignKey(
        Warehouse, related_name="+", on_delete=models.CASCADE
    )
    destination_warehouse = models.ForeignKey(
        Warehouse,
        null=True,
        blank=True,
        related_name="+",
        on_delete=models.CASCADE,
    )
    product = models.ForeignKey(Product, related_name="stock_movements", on_delete=models.CASCADE)
    movement_type = models.CharField(max_length=20, choices=MOVEMENT_TYPES)
    quantity = models.DecimalField(max_digits=14, decimal_places=2)
    movement_date = models.DateField()
    notes = models.CharField(max_length=255, blank=True, default="")
    reference_type = models.CharField(max_length=40, blank=True, default="")
    reference_id = models.CharField(max_length=40, blank=True, default="")
    created_by = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        null=True,
        blank=True,
        related_name="+",
        on_delete=models.SET_NULL,
    )
    paired_movement = models.OneToOneField(
        "self",
        null=True,
        blank=True,
        related_name="+",
        on_delete=models.SET_NULL,
    )

    class Meta:
        ordering = ["-movement_date", "-id"]

    def __str__(self):
        return f"{self.movement_type} {self.quantity:g} x {self.product} @ {self.warehouse}"