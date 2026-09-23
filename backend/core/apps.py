"""Core application - shared foundation for the AVMS backend.

Holds cross-cutting utilities that all feature apps depend on:
base models, common managers, API response helpers and the health view.
"""

from django.apps import AppConfig


class CoreConfig(AppConfig):
    default_auto_field = "django.db.models.BigAutoField"
    name = "core"
    verbose_name = "Core"