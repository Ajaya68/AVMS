"""Seed the built-in AVMS roles and permissions.

Usage:

    python manage.py seed_roles

The command is idempotent: it updates existing roles with the current
permission matrix and creates anything missing. Safe to run repeatedly.
"""

from django.core.management.base import BaseCommand

from accounts.models import Permission, Role

# module -> (code, name) capability list for every module in the system.
MODULES = {
    "dashboard": [("dashboard.view", "View dashboard")],
    "ventures": [
        ("ventures.view", "View ventures"),
        ("ventures.manage", "Manage ventures"),
    ],
    "customers": [
        ("customers.view", "View customers"),
        ("customers.manage", "Manage customers"),
    ],
    "suppliers": [
        ("suppliers.view", "View suppliers"),
        ("suppliers.manage", "Manage suppliers"),
    ],
    "products": [
        ("products.view", "View products"),
        ("products.manage", "Manage products"),
    ],
    "categories": [
        ("categories.view", "View categories"),
        ("categories.manage", "Manage categories"),
    ],
    "units": [
        ("units.view", "View units"),
        ("units.manage", "Manage units"),
    ],
    "warehouses": [
        ("warehouses.view", "View warehouses"),
        ("warehouses.manage", "Manage warehouses"),
    ],
    "inventory": [
        ("inventory.view", "View inventory"),
        ("inventory.manage", "Manage inventory"),
    ],
    "stock_movements": [
        ("stock_movements.view", "View stock movements"),
        ("stock_movements.manage", "Manage stock movements"),
    ],
    "sales": [
        ("sales.view", "View sales"),
        ("sales.manage", "Manage sales"),
    ],
    "sales_returns": [
        ("sales_returns.view", "View sales returns"),
        ("sales_returns.manage", "Manage sales returns"),
    ],
    "purchases": [
        ("purchases.view", "View purchases"),
        ("purchases.manage", "Manage purchases"),
    ],
    "purchase_returns": [
        ("purchase_returns.view", "View purchase returns"),
        ("purchase_returns.manage", "Manage purchase returns"),
    ],
    "payments": [
        ("payments.view", "View payments"),
        ("payments.manage", "Manage payments"),
    ],
    "expenses": [
        ("expenses.view", "View expenses"),
        ("expenses.manage", "Manage expenses"),
    ],
    "employees": [
        ("employees.view", "View employees"),
        ("employees.manage", "Manage employees"),
    ],
    "reports": [("reports.view", "View reports")],
    "users": [
        ("users.view", "View users"),
        ("users.manage", "Manage users"),
    ],
    "roles": [
        ("roles.view", "View roles"),
        ("roles.manage", "Manage roles"),
    ],
    "notifications": [
        ("notifications.view", "View notifications"),
        ("notifications.manage", "Manage notifications"),
    ],
    "audit": [("audit.view", "View audit logs")],
    "settings": [
        ("settings.view", "View settings"),
        ("settings.manage", "Manage settings"),
    ],
}

ADMIN = ["dashboard.view", "ventures.view", "ventures.manage",
         "customers.view", "customers.manage",
         "suppliers.view", "suppliers.manage",
         "products.view", "products.manage",
         "categories.view", "categories.manage",
         "units.view", "units.manage",
         "warehouses.view", "warehouses.manage",
         "inventory.view", "inventory.manage",
         "stock_movements.view", "stock_movements.manage",
         "sales.view", "sales.manage",
         "sales_returns.view", "sales_returns.manage",
         "purchases.view", "purchases.manage",
         "purchase_returns.view", "purchase_returns.manage",
         "payments.view", "payments.manage",
         "expenses.view", "expenses.manage",
         "employees.view", "employees.manage",
         "reports.view",
         "users.view", "users.manage",
         "roles.view", "roles.manage",
         "notifications.view", "notifications.manage",
         "audit.view",
         "settings.view", "settings.manage"]

MANAGER = ["dashboard.view", "ventures.view",
           "customers.view", "customers.manage",
           "suppliers.view", "suppliers.manage",
           "products.view", "products.manage",
           "categories.view", "categories.manage",
           "units.view", "units.manage",
           "warehouses.view", "warehouses.manage",
           "inventory.view", "inventory.manage",
           "stock_movements.view", "stock_movements.manage",
           "sales.view", "sales.manage",
           "sales_returns.view", "sales_returns.manage",
           "purchases.view", "purchases.manage",
           "purchase_returns.view", "purchase_returns.manage",
           "payments.view", "expenses.view",
           "employees.view", "reports.view",
           "notifications.view"]

ACCOUNTANT = ["dashboard.view",
              "sales.view", "sales_returns.view",
              "purchases.view", "purchase_returns.view",
              "payments.view", "payments.manage",
              "expenses.view", "expenses.manage",
              "reports.view"]

SALES_STAFF = ["dashboard.view",
               "customers.view", "customers.manage",
               "sales.view", "sales.manage",
               "sales_returns.view", "sales_returns.manage",
               "payments.view"]

INVENTORY_STAFF = ["dashboard.view",
                   "products.view", "products.manage",
                   "categories.view", "units.view",
                   "warehouses.view", "warehouses.manage",
                   "inventory.view", "inventory.manage",
                   "stock_movements.view", "stock_movements.manage"]

EMPLOYEE = ["dashboard.view"]

ROLE_MATRIX = {
    "ADMIN": {"name": "Administrator", "description": "Full system access", "permissions": ADMIN},
    "MANAGER": {"name": "Manager", "description": "Sales, purchases, inventory, customers, suppliers, reports", "permissions": MANAGER},
    "ACCOUNTANT": {"name": "Accountant", "description": "Payments, expenses and financial reports", "permissions": ACCOUNTANT},
    "SALES_STAFF": {"name": "Sales Staff", "description": "Customers and sales", "permissions": SALES_STAFF},
    "INVENTORY_STAFF": {"name": "Inventory Staff", "description": "Products, inventory and stock movements", "permissions": INVENTORY_STAFF},
    "EMPLOYEE": {"name": "Employee", "description": "Limited employee functionality", "permissions": EMPLOYEE},
}


class Command(BaseCommand):
    help = "Seed (or update) the built-in AVMS roles and permissions."

    def handle(self, *args, **options):
        # 1. Permissions
        permissions = {}
        for module, caps in MODULES.items():
            for code, name in caps:
                perm, _ = Permission.objects.update_or_create(
                    code=code,
                    defaults={"name": name, "module": module},
                )
                permissions[code] = perm
        self.stdout.write(self.style.SUCCESS(f"Seeded {len(permissions)} permissions."))

        # 2. Roles
        for code, info in ROLE_MATRIX.items():
            role, created = Role.objects.update_or_create(
                code=code,
                defaults={
                    "name": info["name"],
                    "description": info["description"],
                    "is_active": True,
                },
            )
            role.permissions.set(
                [permissions[c] for c in info["permissions"] if c in permissions]
            )
            status = "created" if created else "updated"
            self.stdout.write(self.style.SUCCESS(f"{status}: {role.name} ({role.code})"))

        # 3. Ensure any legacy/inactive roles remain intact
        self.stdout.write(self.style.SUCCESS("Done."))