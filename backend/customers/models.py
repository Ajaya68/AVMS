from django.db import models

from core.models import TimeStampedModel
from core.services import generate_code

from ventures.models import Venture


class Customer(TimeStampedModel):
    STATUS_ACTIVE = "ACTIVE"
    STATUS_INACTIVE = "INACTIVE"
    STATUS_CHOICES = [
        (STATUS_ACTIVE, "Active"),
        (STATUS_INACTIVE, "Inactive"),
    ]

    venture = models.ForeignKey(Venture, related_name="customers", on_delete=models.CASCADE)
    customer_code = models.CharField(max_length=20, editable=False)
    name = models.CharField(max_length=150)
    phone = models.CharField(max_length=20, blank=True, default="")
    email = models.EmailField(blank=True, default="")
    address = models.CharField(max_length=255, blank=True, default="")
    city = models.CharField(max_length=100, blank=True, default="")
    state = models.CharField(max_length=100, blank=True, default="")
    pincode = models.CharField(max_length=10, blank=True, default="")
    gst_number = models.CharField(max_length=20, blank=True, default="")
    credit_limit = models.DecimalField(max_digits=12, decimal_places=2, default=0)
    status = models.CharField(max_length=10, choices=STATUS_CHOICES, default=STATUS_ACTIVE)

    class Meta:
        ordering = ["name"]
        constraints = [
            models.UniqueConstraint(
                fields=["venture", "customer_code"], name="uniq_customer_code_per_venture"
            )
        ]

    def __str__(self):
        return f"{self.customer_code} - {self.name}"

    def save(self, *args, **kwargs):
        if not self.customer_code:
            self.customer_code = generate_code("customers", self.venture_id, "C")
        super().save(*args, **kwargs)