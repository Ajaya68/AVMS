"""Custom user, role and permission models for AVMS.

A minimal email-based user is introduced in Phase 1 so that the initial
migration set is created with ``AUTH_USER_MODEL`` already pointing at a
custom model. Swapping the user model after migrations exist would
require rebuilding the database, so this decision is made up front.

Phase 2 layers roles and capabilities on top:

- ``Permission`` is a coarse capability code (e.g. ``customers.manage``).
- ``Role`` groups permissions (ADMIN, MANAGER, ACCOUNTANT, ...).
- ``User.roles`` assigns roles to users.

Authorization is enforced in the backend via permission classes; the
frontend only ever reflects what the backend reports.
"""

from django.contrib.auth.base_user import AbstractBaseUser, BaseUserManager
from django.contrib.auth.models import PermissionsMixin
from django.db import models


class UserManager(BaseUserManager):
    use_in_migrations = True

    def _create_user(self, email, password, **extra_fields):
        if not email:
            raise ValueError("The email address must be set")
        email = self.normalize_email(email)
        user = self.model(email=email, **extra_fields)
        user.set_password(password)
        user.save(using=self._db)
        return user

    def create_user(self, email, password=None, **extra_fields):
        extra_fields.setdefault("is_staff", False)
        extra_fields.setdefault("is_superuser", False)
        return self._create_user(email, password, **extra_fields)

    def create_superuser(self, email, password=None, **extra_fields):
        extra_fields.setdefault("is_staff", True)
        extra_fields.setdefault("is_superuser", True)

        if extra_fields.get("is_staff") is not True:
            raise ValueError("Superuser must have is_staff=True.")
        if extra_fields.get("is_superuser") is not True:
            raise ValueError("Superuser must have is_superuser=True.")
        return self._create_user(email, password, **extra_fields)


class User(AbstractBaseUser, PermissionsMixin):
    """Identity model for everyone using AVMS."""

    email = models.EmailField(unique=True)
    full_name = models.CharField(max_length=255, blank=True, default="")
    phone = models.CharField(max_length=20, blank=True, default="")

    is_active = models.BooleanField(default=True)
    is_staff = models.BooleanField(default=False)
    is_superuser = models.BooleanField(default=False)

    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    objects = UserManager()

    USERNAME_FIELD = "email"
    REQUIRED_FIELDS = []

    roles = models.ManyToManyField(
        "Role",
        related_name="users",
        blank=True,
    )

    class Meta:
        ordering = ["email"]
        verbose_name = "User"
        verbose_name_plural = "Users"

    def __str__(self):
        return self.email

    def get_full_name(self):
        return self.full_name or self.email

    def get_short_name(self):
        return self.email

    # --- Role / permission helpers -----------------------------------

    @property
    def role_codes(self) -> list[str]:
        return list(
            self.roles.filter(is_active=True).values_list("code", flat=True)
        )

    def has_permission_code(self, code: str) -> bool:
        """True if the user holds ``code`` via an active role, or is a superuser."""
        if self.is_superuser:
            return True
        return self.roles.filter(
            is_active=True, permissions__code=code
        ).exists()

    def permission_codes(self) -> set[str]:
        """All capability codes granted through active roles (superuser: ALL)."""
        codes = {
            code
            for code in self.roles.filter(is_active=True)
            .values_list("permissions__code", flat=True)
            .distinct()
            if code
        }
        if self.is_superuser:
            codes.update(
                Permission.objects.values_list("code", flat=True)
            )
        return codes

    @property
    def permissions(self) -> list[str]:
        """Sorted capability codes, exposed to the API via the user serializer."""
        return sorted(self.permission_codes())


class Permission(models.Model):
    """A coarse capability code that a role may grant."""

    code = models.CharField(max_length=100, unique=True)
    name = models.CharField(max_length=150)
    module = models.CharField(max_length=50, db_index=True)

    class Meta:
        ordering = ["module", "code"]
        verbose_name = "Permission"
        verbose_name_plural = "Permissions"

    def __str__(self):
        return f"{self.name} ({self.code})"


class Role(models.Model):
    """A named group of permissions assigned to users."""

    code = models.CharField(max_length=50, unique=True)
    name = models.CharField(max_length=100)
    description = models.TextField(blank=True, default="")
    permissions = models.ManyToManyField(
        Permission,
        related_name="roles",
        blank=True,
    )
    is_active = models.BooleanField(default=True)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    ROLES = [
        "ADMIN",
        "MANAGER",
        "ACCOUNTANT",
        "SALES_STAFF",
        "INVENTORY_STAFF",
        "EMPLOYEE",
    ]

    class Meta:
        ordering = ["code"]
        verbose_name = "Role"
        verbose_name_plural = "Roles"

    @property
    def permission_codes(self) -> list[str]:
        """Sorted capability codes, exposed via the role serializer."""
        return sorted(self.permissions.values_list("code", flat=True))

    def __str__(self):
        return f"{self.name} ({self.code})"