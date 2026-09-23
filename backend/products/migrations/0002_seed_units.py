"""Seed the static list of measurement units."""

from django.db import migrations

UNITS = [
    ("kg", "Kilogram", True),
    ("g", "Gram", False),
    ("pcs", "Pieces", True),
    ("packet", "Packet", False),
    ("litre", "Litre", True),
    ("box", "Box", False),
]


def seed_units(apps, schema_editor):
    Unit = apps.get_model("products", "Unit")
    for code, name, is_base in UNITS:
        Unit.objects.get_or_create(unit_code=code, defaults={"unit_name": name, "is_base": is_base})


def unseed_units(apps, schema_editor):
    Unit = apps.get_model("products", "Unit")
    Unit.objects.filter(unit_code__in=[u[0] for u in UNITS]).delete()


class Migration(migrations.Migration):

    dependencies = [
        ("products", "0001_initial"),
    ]

    operations = [
        migrations.RunPython(seed_units, unseed_units),
    ]