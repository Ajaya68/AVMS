"""Cross-module regression and end-to-end scenarios.

Phase 13: locks in fixes that were previously found live (string
``reorder_level`` crash, notification audiences, venture-scoped detail
reads) and exercises a few full business flows end to end.
"""

from datetime import date
from decimal import Decimal

from django.contrib.auth import get_user_model
from rest_framework.test import APIClient, APITestCase

from accounts.models import Permission, Role
from audit.models import AuditLog
from core.models import SequenceCounter
from customers.models import Customer
from expenses.models import Expense, ExpenseCategory
from inventory.models import Inventory, StockMovement, Warehouse
from inventory.services import record_stock_movement
from notifications.models import Notification
from payments.models import Payment
from products.models import Category, Product, Unit
from purchases.models import Purchase
from sales.models import Sale
from suppliers.models import Supplier
from ventures.models import Venture, VentureCodeCounter

User = get_user_model()


class RegressionTests(APITestCase):
    def setUp(self):
        SequenceCounter.objects.all().delete()
        VentureCodeCounter.objects.all().delete()
        for model in [
            Payment, Expense, Purchase, Sale, StockMovement, Inventory,
            Customer, Supplier, Product, Category, Warehouse, Notification,
            AuditLog, Venture,
        ]:
            model.objects.all().delete()

        self.admin = User.objects.create_superuser(
            email="admin@avms.local", password="Admin@12345"
        )
        self.staff = User.objects.create_user(
            email="staff@avms.local", password="Staff@12345"
        )
        self.manager = User.objects.create_user(
            email="manager@avms.local", password="Manager@12345"
        )
        manage_sales, _ = Permission.objects.get_or_create(
            code="sales.manage", defaults={"name": "Manage sales", "module": "sales"}
        )
        manage_inventory, _ = Permission.objects.get_or_create(
            code="inventory.manage",
            defaults={"name": "Manage inventory", "module": "inventory"},
        )
        view_notifications, _ = Permission.objects.get_or_create(
            code="notifications.view",
            defaults={"name": "View notifications", "module": "notifications"},
        )
        role = Role.objects.create(code="MANAGER", name="Manager")
        role.permissions.add(manage_sales, manage_inventory, view_notifications)
        self.manager.roles.add(role)

        self.venture = Venture.objects.create(venture_name="Mushroom Farm")
        self.other_venture = Venture.objects.create(venture_name="Tea Estate")
        self.wh = Warehouse.objects.create(
            venture=self.venture, warehouse_name="Main Godown"
        )
        self.other_wh = Warehouse.objects.create(
            venture=self.other_venture, warehouse_name="Other Godown"
        )
        self.category = Category.objects.create(
            venture=self.venture, category_name="Produce"
        )
        self.other_category = Category.objects.create(
            venture=self.other_venture, category_name="Beverages"
        )
        self.expense_category = ExpenseCategory.objects.create(
            category_code="FUEL", category_name="Fuel"
        )
        self.product = Product.objects.create(
            venture=self.venture, category=self.category,
            unit=Unit.objects.get(unit_code="kg"),
            product_name="Button Mushroom", purchase_price="120",
            selling_price="200", reorder_level="5",
        )
        self.supplier = Supplier.objects.create(
            venture=self.venture, name="Agro Traders"
        )
        self.customer = Customer.objects.create(
            venture=self.venture, name="Ravi Stores"
        )

        self.client = APIClient()
        self.client.force_authenticate(self.admin)
        self.client.credentials(HTTP_X_VENTURE_ID=str(self.venture.id))

    # --- Regression: reorder_level kept as a str in memory ----------------

    def test_low_stock_alert_with_in_memory_string_reorder_level(self):
        # Product.reorder_level was created from a string and, before the fix,
        # stayed a str on the python instance, so comparing Decimal <= str in
        # _alert_low_stock raised a TypeError instead of firing the alert.
        record_stock_movement(
            None,
            venture=self.venture, warehouse=self.wh, product=self.product,
            movement_type=StockMovement.MOVE_ADJUSTMENT_IN, quantity="10",
        )
        record_stock_movement(
            None,
            venture=self.venture, warehouse=self.wh, product=self.product,
            movement_type=StockMovement.MOVE_ADJUSTMENT_OUT, quantity="8",
        )

        alerts = Notification.objects.filter(type=Notification.TYPE_LOW_STOCK)
        self.assertGreaterEqual(alerts.count(), 1)
        self.assertIn("Button Mushroom", alerts.first().message)
        row = Inventory.objects.get(warehouse=self.wh, product=self.product)
        self.assertEqual(Decimal(row.available), Decimal("2"))

    # --- Regression: notification audience ---------------------------------

    def test_sale_created_notifies_superuser_and_perm_holder_only(self):
        record_stock_movement(
            None,
            venture=self.venture, warehouse=self.wh, product=self.product,
            movement_type=StockMovement.MOVE_ADJUSTMENT_IN, quantity="50",
        )
        payload = {
            "venture": self.venture.id,
            "customer": self.customer.id,
            "warehouse": self.wh.id,
            "sale_date": "2024-08-05",
            "items": [
                {"product": self.product.id, "quantity": "3", "unit_price": "200"}
            ],
        }
        resp = self.client.post("/api/sales/", payload, format="json")
        self.assertEqual(resp.status_code, 201, resp.data)
        recipients = set(
            Notification.objects.filter(type=Notification.TYPE_SALE_CREATED)
            .values_list("user__email", flat=True)
        )
        self.assertIn("admin@avms.local", recipients)
        self.assertIn("manager@avms.local", recipients)
        self.assertNotIn("staff@avms.local", recipients)

    # --- Regression: notifications are owner-scoped ------------------------

    def test_notification_mark_read_is_owner_scoped(self):
        from notifications.services import notify

        note = notify(self.admin, Notification.TYPE_SYSTEM, "secret for admin")
        mgr_client = APIClient()
        mgr_client.force_authenticate(self.manager)
        mgr_client.credentials(HTTP_X_VENTURE_ID=str(self.venture.id))

        # Manager can list/read notifications in general (has notifications.view)
        # but must not touch another user's inbox.
        self.assertEqual(mgr_client.post(f"/api/notifications/{note.id}/read/").status_code, 404)
        note.refresh_from_db()
        self.assertFalse(note.is_read)
        data = mgr_client.get("/api/notifications/").data["data"]
        self.assertEqual(data["count"], 0)
        data = mgr_client.get("/api/notifications/unread-count/").data["data"]
        self.assertEqual(data["count"], 0)

    # --- Regression: detail endpoints are venture-scoped -------------------

    def test_foreign_venture_records_invisible_via_detail(self):
        other_customer = Customer.objects.create(
            venture=self.other_venture, name="Tea Buyer"
        )
        other_product = Product.objects.create(
            venture=self.other_venture, category=self.other_category,
            unit=Unit.objects.get(unit_code="kg"),
            product_name="Assam Tea", purchase_price="300",
            selling_price="450", reorder_level="10",
        )
        other_expense = Expense.objects.create(
            venture=self.other_venture, category=self.expense_category,
            amount="100", expense_date=date.today(),
        )
        other_sale = Sale.objects.create(
            venture=self.other_venture, customer=other_customer,
            warehouse=self.other_wh, sale_date=date.today(),
        )

        # MasterEntityPage-powered module (customer)
        self.assertEqual(
            self.client.get(f"/api/customers/{other_customer.id}/").status_code, 404
        )
        # Produces (product detail)
        self.assertEqual(
            self.client.get(f"/api/products/{other_product.id}/").status_code, 404
        )
        # Expenses
        self.assertEqual(
            self.client.get(f"/api/expenses/{other_expense.id}/").status_code, 404
        )
        # Sales + item listing
        self.assertEqual(
            self.client.get(f"/api/sales/{other_sale.id}/").status_code, 404
        )
        self.assertEqual(
            self.client.get(f"/api/sales/{other_sale.id}/items/").status_code, 404
        )

    # --- Full business flow: purchase -> payment -> settle -> overpay ------

    def test_purchase_settled_then_over_payment_rejected(self):
        purchase_payload = {
            "venture": self.venture.id,
            "supplier": self.supplier.id,
            "warehouse": self.wh.id,
            "purchase_date": "2024-08-02",
            "items": [
                {"product": self.product.id, "quantity": "10", "unit_price": "120"}
            ],
        }
        resp = self.client.post("/api/purchases/", purchase_payload, format="json")
        self.assertEqual(resp.status_code, 201, resp.data)
        purchase_id = resp.data["data"]["id"]
        self.assertEqual(Decimal(resp.data["data"]["due_amount"]), Decimal("1200"))

        pay_payload = lambda amount: {
            "venture": self.venture.id,
            "payment_type": "PAID",
            "reference_type": "PURCHASE",
            "reference_id": purchase_id,
            "amount": amount,
            "payment_date": "2024-08-05",
            "payment_method": "UPI",
        }
        settle = self.client.post("/api/payments/", pay_payload("1200"), format="json")
        self.assertEqual(settle.status_code, 201, settle.data)
        purchase = Purchase.objects.get(pk=purchase_id)
        self.assertEqual(Decimal(purchase.due_amount), Decimal("0"))
        self.assertEqual(Decimal(purchase.paid_amount), Decimal("1200"))

        overpay = self.client.post(
            "/api/payments/", pay_payload("10"), format="json"
        )
        self.assertEqual(overpay.status_code, 400, overpay.data)
        self.assertIn("outstanding balance", overpay.data["message"])
        self.assertEqual(Payment.objects.count(), 1)