"""Venture model - the top-level business unit.

Every business-facing model hangs off a Venture via a foreign key, which is
what provides multi-venture data isolation (see core/service helpers in
``ventures/services.py``).
"""

from django.db import models, transaction
from django.db.models import F

from core.models import TimeStampedModel


class VentureCodeCounter(models.Model):
    """Single-row counter that hands out the next venture code.

    The code is decoupled from the primary key so that deletions (or gaps in
    auto-increment) never reuse or shift numbers. Reads are serialized with a
    row lock, making concurrent creation safe.
    """

    id = models.PositiveIntegerField(primary_key=True, default=1)
    next_value = models.PositiveIntegerField(default=1)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        verbose_name = "Venture Code Counter"
        verbose_name_plural = "Venture Code Counters"


class VentureManager(models.Manager):
    def generate_code(self):
        """Return the next sequential venture code (``V-0001`` style).

        Serialized via ``SELECT ... FOR UPDATE`` so duplicate codes cannot be
        handed out under concurrent creation.
        """
        with transaction.atomic():
            counter = (VentureCodeCounter.objects.select_for_update()
                       .filter(id=1).first())
            if counter is None:
                counter, _ = VentureCodeCounter.objects.get_or_create(
                    id=1, defaults={"next_value": 1}
                )
            value = counter.next_value
            counter.next_value = F("next_value") + 1
            counter.save(update_fields=["next_value"])
        return f"V-{value:04d}"


class Venture(TimeStampedModel):
    """An enterprise unit operated within AVMS."""

    class Status(models.TextChoices):
        ACTIVE = "ACTIVE", "Active"
        INACTIVE = "INACTIVE", "Inactive"

    class BusinessType(models.TextChoices):
        MUSHROOM = "MUSHROOM", "Mushroom"
        FISH_FARMING = "FISH_FARMING", "Fish Farming"
        AGRICULTURE = "AGRICULTURE", "Agriculture"
        POULTRY = "POULTRY", "Poultry"
        DAIRY = "DAIRY", "Dairy"
        GENERAL = "GENERAL", "General"
        OTHER = "OTHER", "Other"

    venture_code = models.CharField(
        max_length=20, unique=True, editable=False
    )
    venture_name = models.CharField(max_length=200)
    description = models.TextField(blank=True, default="")
    business_type = models.CharField(
        max_length=30,
        choices=BusinessType.choices,
        default=BusinessType.GENERAL,
    )
    phone = models.CharField(max_length=20, blank=True, default="")
    email = models.EmailField(blank=True, default="")
    address = models.CharField(max_length=255, blank=True, default="")
    city = models.CharField(max_length=100, blank=True, default="")
    state = models.CharField(max_length=100, blank=True, default="")
    pincode = models.CharField(max_length=10, blank=True, default="")
    status = models.CharField(
        max_length=10,
        choices=Status.choices,
        default=Status.ACTIVE,
        db_index=True,
    )

    objects = VentureManager()

    class Meta:
        ordering = ["venture_code"]
        verbose_name = "Venture"
        verbose_name_plural = "Ventures"

    def save(self, *args, **kwargs):
        if not self.venture_code:
            self.venture_code = self.__class__.objects.generate_code()
        super().save(*args, **kwargs)

    def __str__(self):
        return f"{self.venture_code} - {self.venture_name}"