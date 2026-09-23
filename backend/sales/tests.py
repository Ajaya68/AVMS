from decimal import Decimal

from django.contrib.auth import get_user_model
from rest_framework.test import APIClient, APITestCase

from audit.models import AuditLog
from core.models import SequenceCounter
from customers.models import Customer
from inventory.models import Inventory, StockMovement, Warehouse
from products.models import Category, Product, Unit
from ventures.models import Venture, VentureCodeCounter

from .models import Sale, SaleReturn

User = get_user_model()


def reset_state():
    SequenceCounter.objects.filter(
        module__in=["sales", "sale_returns", "warehouses"]
    ).delete()
    StockMovement.objects.all().delete()
    Inventory.objects.all().delete()
    SaleReturn.objects.all().delete()
    Sale.objects.all().delete()
    Product.objects.all().delete()
    Category.objects.all().delete()
    Customer.objects.all().delete()
    Warehouse.objects.all().delete()
    VentureCodeCounter.objects.all().delete()
    Venture.objects.all().delete()


class SaleTests(APITestCase):
    def setUp(self):
        reset_state()
        self.client = APIClient()
        self.admin = User.objects.create_superuser(
            email="admin@avms.local", password="Admin@12345"
        )
        self.venture = Venture.objects.create(venture_name="Mushroom Farm")
        self.venture2 = Venture.objects.create(venture_name="Fish Pond")
        self.wh = Warehouse.objects.create(
            venture=self.venture, warehouse_name="Main Godown", city="Nalbari"
        )
        self.customer = Customer.objects.create(
            venture=self.venture, name="Ravi Stores", phone="9876543210"
        )
        self.category = Category.objects.create(
            venture=self.venture, category_name="Mushroom"
        )
        self.product = Product.objects.create(
            venture=self.venture,
            category=self.category,
            unit=Unit.objects.get(unit_code="kg"),
            product_name="Button Mushroom",
            purchase_price="120",
            selling_price="180",
            reorder_level="5",
        )
        self.client.force_authenticate(self.admin)

    def _stock(self, quantity="100"):
        return self.client.post(
            "/api/stock-movements/",
            {
                "warehouse": self.wh.id,
                "product": self.product.id,
                "movement_type": "PURCHASE",
                "quantity": quantity,
            },
            format="json",
        )

    def _sale(self, quantity="10", unit_price="180", **overrides):
        payload = {
            "venture": self.venture.id,
            "customer": self.customer.id,
            "warehouse": self.wh.id,
            "sale_date": "2024-08-01",
            "items": [
                {"product": self.product.id, "quantity": quantity, "unit_price": unit_price}
            ],
        }
        payload.update(overrides)
        return self.client.post("/api/sales/", payload, format="json")

    def test_create_sale_auto_code_totals_and_stock_out(self):
        self._stock("100")
        resp = self._sale(
            quantity="10", unit_price="180",
            discount="100", tax="50", paid_amount="1000",
        )
        self.assertEqual(resp.status_code, 201, resp.data)
        data = resp.data["data"]
        self.assertEqual(data["invoice_number"], "SINV-0001")
        self.assertEqual(Decimal(data["subtotal"]), Decimal("1800"))
        self.assertEqual(Decimal(data["total_amount"]), Decimal("1750"))
        self.assertEqual(Decimal(data["due_amount"]), Decimal("750"))
        self.assertEqual(
            Inventory.objects.get(warehouse=self.wh, product=self.product).quantity,
            Decimal("90"),
        )
        self.assertEqual(StockMovement.objects.filter(movement_type="SALE").count(), 1)

    def test_sale_rejects_insufficient_stock(self):
        self._stock("5")
        resp = self._sale(quantity="10")
        self.assertEqual(resp.status_code, 400)
        self.assertIn("Insufficient stock", resp.data["message"])
        self.assertEqual(Sale.objects.count(), 0)
        self.assertEqual(
            Inventory.objects.get(warehouse=self.wh, product=self.product).quantity,
            Decimal("5"),
        )

    def test_sale_without_warehouse_skips_stock(self):
        resp = self._sale(warehouse=None)
        # no warehouse -> no movement attempted -> allowed
        self.assertEqual(resp.status_code, 201, resp.data)
        self.assertEqual(StockMovement.objects.filter(movement_type="SALE").count(), 0)
        pk = resp.data["data"]["id"]
        self.assertEqual(self.client.delete(f"/api/sales/{pk}/").status_code, 200)

    def test_delete_dispatched_sale_blocked(self):
        self._stock("50")
        resp = self._sale()
        self.assertEqual(resp.status_code, 201, resp.data)
        pk = resp.data["data"]["id"]
        resp = self.client.delete(f"/api/sales/{pk}/")
        self.assertEqual(resp.status_code, 400)
        self.assertIn("reverse it with a sale return", resp.data["message"])

    def test_cross_venture_customer_rejected(self):
        other_customer = Customer.objects.create(venture=self.venture2, name="Other Shop")
        resp = self._sale(customer=other_customer.id)
        self.assertEqual(resp.status_code, 400, resp.data)
        self.assertIn("venture", resp.data["errors"]["customer"][0])

    def test_sale_list_search_and_status(self):
        self._stock("100")
        self._sale()
        self.assertEqual(self.client.get("/api/sales/?search=SINV").data["data"]["count"], 1)
        self.assertEqual(self.client.get("/api/sales/?status=COMPLETED").data["data"]["count"], 1)
        self.assertEqual(self.client.get("/api/sales/?status=BOGUS").data["data"]["count"], 0)

    def test_sale_items_endpoint(self):
        self._stock("100")
        resp = self._sale()
        pk = resp.data["data"]["id"]
        items = self.client.get(f"/api/sales/{pk}/items/").data["data"]
        self.assertEqual(len(items), 1)

    def test_sale_audited(self):
        self._stock("100")
        self._sale()
        entry = AuditLog.objects.filter(module="sales").first()
        self.assertIsNotNone(entry)
        self.assertEqual(entry.action, AuditLog.ACTION_CREATE)


class SaleReturnTests(APITestCase):
    def setUp(self):
        reset_state()
        self.client = APIClient()
        self.admin = User.objects.create_superuser(
            email="admin@avms.local", password="Admin@12345"
        )
        self.venture = Venture.objects.create(venture_name="Mushroom Farm")
        self.wh = Warehouse.objects.create(
            venture=self.venture, warehouse_name="Main Godown", city="Nalbari"
        )
        self.customer = Customer.objects.create(
            venture=self.venture, name="Ravi Stores", phone="9876543210"
        )
        self.product = Product.objects.create(
            venture=self.venture,
            unit=Unit.objects.get(unit_code="kg"),
            product_name="Button Mushroom",
            selling_price="180",
            reorder_level="5",
        )
        self.client.force_authenticate(self.admin)

        # stock in 100 first
        self.client.post(
            "/api/stock-movements/",
            {
                "warehouse": self.wh.id,
                "product": self.product.id,
                "movement_type": "PURCHASE",
                "quantity": "100",
            },
            format="json",
        )
        created = self.client.post(
            "/api/sales/",
            {
                "venture": self.venture.id,
                "customer": self.customer.id,
                "warehouse": self.wh.id,
                "sale_date": "2024-08-01",
                "items": [
                    {"product": self.product.id, "quantity": "40", "unit_price": "180"}
                ],
            },
            format="json",
        )
        self.sale = Sale.objects.get(pk=created.data["data"]["id"])

    def _return(self, quantity="10"):
        return self.client.post(
            "/api/sales-returns/",
            {
                "venture": self.venture.id,
                "sale": self.sale.id,
                "return_date": "2024-08-05",
                "items": [
                    {"product": self.product.id, "quantity": quantity, "unit_price": "180"}
                ],
            },
            format="json",
        )

    def test_partial_return_restores_stock_and_balances(self):
        resp = self._return("10")
        self.assertEqual(resp.status_code, 201, resp.data)
        self.assertEqual(resp.data["data"]["return_number"], "SRET-0001")
        # 100 - 40 sold = 60, + 10 returned = 70
        self.assertEqual(
            Inventory.objects.get(warehouse=self.wh, product=self.product).quantity,
            Decimal("70"),
        )
        self.sale.refresh_from_db()
        self.assertEqual(Decimal(self.sale.returned_amount), Decimal("1800"))
        self.assertEqual(Decimal(self.sale.due_amount), Decimal("5400"))
        self.assertEqual(self.sale.status, "PARTIAL")

    def test_full_return_sets_status_returned(self):
        self._return("40")
        self.sale.refresh_from_db()
        self.assertEqual(self.sale.status, "RETURNED")
        self.assertEqual(Decimal(self.sale.due_amount), Decimal("0"))
        # 100 - 40 sold + 40 returned
        self.assertEqual(
            Inventory.objects.get(warehouse=self.wh, product=self.product).quantity,
            Decimal("100"),
        )

    def test_cannot_return_more_than_sold(self):
        resp = self._return("50")
        self.assertEqual(resp.status_code, 400, resp.data)
        self.assertIn("exceeds sold quantity", resp.data["message"])

    def test_return_rejects_product_not_in_sale(self):
        other = Product.objects.create(
            venture=self.venture,
            unit=Unit.objects.get(unit_code="kg"),
            product_name="Oyster Mushroom",
        )
        resp = self.client.post(
            "/api/sales-returns/",
            {
                "venture": self.venture.id,
                "sale": self.sale.id,
                "return_date": "2024-08-05",
                "items": [
                    {"product": other.id, "quantity": "1", "unit_price": "200"}
                ],
            },
            format="json",
        )
        self.assertEqual(resp.status_code, 400, resp.data)
        self.assertIn("was not part of this sale", resp.data["errors"]["items.0.product"][0])

    def test_sale_return_audited(self):
        self._return("5")
        entry = AuditLog.objects.filter(module="sales_returns").first()
        self.assertIsNotNone(entry)


class SalesPermissionsTests(APITestCase):
    def setUp(self):
        reset_state()
        self.client = APIClient()
        self.staff = User.objects.create_user(
            email="staff@avms.local", password="Staff@12345"
        )
        self.client.force_authenticate(self.staff)

    def test_staff_forbidden(self):
        self.assertEqual(self.client.get("/api/sales/").status_code, 403)
        self.assertEqual(self.client.get("/api/sales-returns/").status_code, 403)
        self.assertEqual(
            self.client.post("/api/sales/", {}, format="json").status_code, 403
        )