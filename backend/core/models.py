"""Base models shared across AVMS feature apps."""

from django.db import models


class TimeStampedModel(models.Model):
    """Adds created_at / updated_at timestamps to a model."""

    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        abstract = True
        ordering = ["-created_at"]


class SequenceCounter(models.Model):
    """Per-(module, venture) sequential code counter.

    Serialized via ``SELECT ... FOR UPDATE`` so concurrent creations cannot
    receive duplicate codes (see the venture-code bug where MySQL auto-increment
    gaps shifted codes). Rows survive row deletes, so codes never repeat.
    """

    module = models.CharField(max_length=40)
    venture = models.ForeignKey(
        "ventures.Venture",
        null=True,
        blank=True,
        on_delete=models.CASCADE,
        related_name="+",
    )
    next_value = models.PositiveIntegerField(default=1)

    class Meta:
        constraints = [
            models.UniqueConstraint(
                fields=["module", "venture"], name="uniq_sequence_module_venture"
            )
        ]


class SoftDeleteQuerySet(models.QuerySet):
    """Filters out soft-deleted rows by default."""

    def active(self):
        return self.filter(is_deleted=False)

    def deleted(self):
        return self.filter(is_deleted=True)


class SoftDeleteManager(models.Manager):
    """Default manager that excludes soft-deleted records."""

    def get_queryset(self):
        return SoftDeleteQuerySet(self.model, using=self._db).active()


class AllObjectsManager(models.Manager):
    """Manager that returns every row, including soft-deleted ones."""

    def get_queryset(self):
        return SoftDeleteQuerySet(self.model, using=self._db)


class SoftDeleteModel(models.Model):
    """Adds an is_deleted flag for soft deletion.

    The default manager hides deleted rows; use ``objects_all`` to
    include them. Model classes that use this must declare::

        objects = SoftDeleteManager()
        objects_all = AllObjectsManager()
    """

    is_deleted = models.BooleanField(default=False, db_index=True)
    deleted_at = models.DateTimeField(null=True, blank=True)

    class Meta:
        abstract = True

    def soft_delete(self):
        from django.utils import timezone

        self.is_deleted = True
        self.deleted_at = timezone.now()
        self.save(update_fields=["is_deleted", "deleted_at"])