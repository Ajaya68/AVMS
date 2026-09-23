from django.db import models

from core.models import TimeStampedModel
from core.services import generate_code

from ventures.models import Venture


class Supplier(TimeStampedModel):
    STATUS_ACTIVE = "ACTIVE"
    STATUS_INACTIVE = "INACTIVE"
    STATUS_CHOICES = [
        (STATUS_ACTIVE, "Active"),
        (STATUS_INACTIVE, "Inactive"),
    ]

    venture = models.ForeignKey(Venture, related_name="suppliers", on_delete=models.CASCADE)
    supplier_code = models.CharField(max_length=20, editable=False)
    name = models.CharField(max_length=150)
    contact_person = models.CharField(max_length=150, blank=True, default="")
    phone = models.CharField(max_length=20, blank=True, default="")
    email = models.EmailField(blank=True, default="")
    address = models.CharField(max_length=255, blank=True, default="")
    city = models.CharField(max_length=100, blank=True, default="")
    state = models.CharField(max_length=100, blank=True, default="")
    pincode = models.CharField(max_length=10, blank=True, default="")
    gst_number = models.CharField(max_length=20, blank=True, default="")
    payment_terms = models.CharField(max_length=100, blank=True, default="")
    status = models.CharField(max_length=10, choices=STATUS_CHOICES, default=STATUS_ACTIVE)

    class Meta:
        ordering = ["name"]
        constraints = [
            models.UniqueConstraint(
                fields=["venture", "supplier_code"], name="uniq_supplier_code_per_venture"
            )
        ]

    def __str__(self):
        return f"{self.supplier_code} - {self.name}"

    def save(self, *args, **kwargs):
        if not self.supplier_code:
            self.supplier_code = generate_code("suppliers", self.venture_id, "S")
        super().save(*args, **kwargs)