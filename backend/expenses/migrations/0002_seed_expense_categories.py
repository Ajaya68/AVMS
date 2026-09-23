from django.db import migrations

CATEGORIES = [
    ("ELECTRICITY", "Electricity"),
    ("TRANSPORT", "Transport"),
    ("RENT", "Rent"),
    ("SALARY", "Salary"),
    ("RAW_MATERIALS", "Raw Materials"),
    ("MARKETING", "Marketing"),
    ("MAINTENANCE", "Maintenance"),
    ("OTHER", "Other"),
]


def seed(apps, schema_editor):
    ExpenseCategory = apps.get_model("expenses", "ExpenseCategory")
    for code, name in CATEGORIES:
        ExpenseCategory.objects.update_or_create(
            category_code=code, defaults={"category_name": name}
        )


def unseed(apps, schema_editor):
    ExpenseCategory = apps.get_model("expenses", "ExpenseCategory")
    ExpenseCategory.objects.filter(
        category_code__in=[c for c, _ in CATEGORIES]
    ).delete()


class Migration(migrations.Migration):
    dependencies = [
        ("expenses", "0001_initial"),
    ]

    operations = [
        migrations.RunPython(seed, unseed),
    ]