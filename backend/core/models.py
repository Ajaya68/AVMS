"""Base models shared across AVMS feature apps."""

from django.db import models


class TimeStampedModel(models.Model):
    """Adds created_at / updated_at timestamps to a model."""

    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        abstract = True
        ordering = ["-created_at"]


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