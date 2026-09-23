from django.db import models

from core.models import TimeStampedModel
from core.services import generate_code

from ventures.models import Venture


class Employee(TimeStampedModel):
    STATUS_ACTIVE = "ACTIVE"
    STATUS_INACTIVE = "INACTIVE"
    STATUS_CHOICES = [
        (STATUS_ACTIVE, "Active"),
        (STATUS_INACTIVE, "Inactive"),
    ]

    venture = models.ForeignKey(Venture, related_name="employees", on_delete=models.CASCADE)
    employee_code = models.CharField(max_length=20, editable=False)
    first_name = models.CharField(max_length=100)
    last_name = models.CharField(max_length=100, blank=True, default="")
    phone = models.CharField(max_length=20, blank=True, default="")
    email = models.EmailField(blank=True, default="")
    department = models.CharField(max_length=100, blank=True, default="")
    designation = models.CharField(max_length=100, blank=True, default="")
    joining_date = models.DateField(null=True, blank=True)
    salary = models.DecimalField(max_digits=14, decimal_places=2, default=0)
    status = models.CharField(max_length=10, choices=STATUS_CHOICES, default=STATUS_ACTIVE)

    class Meta:
        ordering = ["first_name", "last_name"]
        constraints = [
            models.UniqueConstraint(
                fields=["venture", "employee_code"], name="uniq_employee_code_per_venture"
            )
        ]

    @property
    def name(self):
        return f"{self.first_name} {self.last_name}".strip()

    def __str__(self):
        return f"{self.employee_code} {self.name}"

    def save(self, *args, **kwargs):
        if not self.employee_code:
            self.employee_code = generate_code("employees", self.venture_id, "EMP")
        super().save(*args, **kwargs)