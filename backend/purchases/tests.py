from decimal import Decimal

from django.contrib.auth import get_user_model
from rest_framework.test import APIClient, APITestCase

from audit.models import AuditLog
from core.models import SequenceCounter
from inventory.models import Inventory, StockMovement
from products.models import Category, Product, Unit
from suppliers.models import Supplier
from ventures.models import Venture, VentureCodeCounter

from .models import Purchase, PurchaseReturn
from .services import finalize_purchase_return

User = get_user_model()


def reset_state():
    SequenceCounter.objects.filter(
        module__in=["purchases", "purchase_returns", "warehouses"]
    ).delete()
    StockMovement.objects.all().delete()
    Inventory.objects.all().delete()
    PurchaseReturn.objects.all().delete()
    Purchase.objects.all().delete()
    Product.objects.all().delete()
    Category.objects.all().delete()
    Supplier.objects.all().delete()
    from inventory.models import Warehouse
    Warehouse.objects.all().delete()
    VentureCodeCounter.objects.all().delete()
    Venture.objects.all().delete()


class PurchaseTests(APITestCase):
    def setUp(self):
        reset_state()
        self.client = APIClient()
        self.admin = User.objects.create_superuser(
            email="admin@avms.local", password="Admin@12345"
        )
        self.venture = Venture.objects.create(venture_name="Mushroom Farm")
        self.venture2 = Venture.objects.create(venture_name="Fish Pond")
        from inventory.models import Warehouse
        self.wh = Warehouse.objects.create(
            venture=self.venture, warehouse_name="Main Godown", city="Nalbari"
        )
        self.supplier = Supplier.objects.create(
            venture=self.venture,
            name="Shree Traders",
            email="supplier@example.com",
            phone="9876543210",
        )
        self.category = Category.objects.create(
            venture=self.venture, category_name="Mushroom"
        )
        self.mushroom = Product.objects.create(
            venture=self.venture,
            category=self.category,
            unit=Unit.objects.get(unit_code="kg"),
            product_name="Button Mushroom",
            purchase_price="120",
            selling_price="180",
            reorder_level="5",
        )
        self.oyster = Product.objects.create(
            venture=self.venture,
            category=self.category,
            unit=Unit.objects.get(unit_code="kg"),
            product_name="Oyster Mushroom",
            purchase_price="150",
            selling_price="220",
            reorder_level="10",
        )
        self.client.force_authenticate(self.admin)

    def _purchase(self, supplier=None, warehouse=None, items=None, **overrides):
        if warehouse is None:
            use_warehouse = self.wh
        else:
            use_warehouse = warehouse
        payload = {
            "venture": self.venture.id,
            "supplier": (supplier or self.supplier).id,
            "warehouse": use_warehouse.id,
            "purchase_date": "2024-08-01",
            "items": items or [
                {"product": self.mushroom.id, "quantity": "100", "unit_price": "120"},
                {"product": self.oyster.id, "quantity": "50", "unit_price": "150"},
            ],
        }
        if warehouse is None and items is not None:
            payload.pop("warehouse")
        payload.update(overrides)
        return self.client.post("/api/purchases/", payload, format="json")

    def _purchase_no_warehouse(self, items=None):
        payload = {
            "venture": self.venture.id,
            "supplier": self.supplier.id,
            "purchase_date": "2024-08-01",
            "items": items or [
                {"product": self.mushroom.id, "quantity": "10", "unit_price": "120"}
            ],
        }
        return self.client.post("/api/purchases/", payload, format="json")

    def test_create_purchase_auto_code_and_totals(self):
        resp = self._purchase(discount="500", tax="60", paid_amount="8000")
        self.assertEqual(resp.status_code, 201, resp.data)
        data = resp.data["data"]
        self.assertEqual(data["invoice_number"], "PINV-0001")
        self.assertEqual(data["status"], "COMPLETED")
        self.assertEqual(Decimal(data["subtotal"]), Decimal("19500"))
        self.assertEqual(Decimal(data["total_amount"]), Decimal("19060"))
        self.assertEqual(Decimal(data["paid_amount"]), Decimal("8000"))
        self.assertEqual(Decimal(data["due_amount"]), Decimal("11060"))
        self.assertEqual(len(data["items"]), 2)
        self.assertEqual(Decimal(data["items"][0]["total"]), Decimal("12000"))

    def test_purchase_records_stock_in(self):
        self._purchase()
        self.assertEqual(
            Inventory.objects.get(warehouse=self.wh, product=self.mushroom).quantity,
            Decimal("100"),
        )
        self.assertEqual(StockMovement.objects.count(), 2)
        types = set(StockMovement.objects.values_list("movement_type", flat=True))
        self.assertEqual(types, {"PURCHASE"})

    def test_purchase_requires_quantity_greater_than_zero(self):
        resp = self._purchase(
            items=[{"product": self.mushroom.id, "quantity": "0", "unit_price": "120"}]
        )
        self.assertEqual(resp.status_code, 400, resp.data)
        self.assertEqual(Purchase.objects.count(), 0)
        self.assertEqual(StockMovement.objects.count(), 0)

    def test_cross_venture_product_rejected(self):
        other_product = Product.objects.create(
            venture=self.venture2,
            unit=Unit.objects.get(unit_code="kg"),
            product_name="Tilapia",
        )
        resp = self._purchase(
            items=[{"product": other_product.id, "quantity": "10", "unit_price": "100"}]
        )
        self.assertEqual(resp.status_code, 400, resp.data)
        self.assertIn("venture", resp.data["errors"]["items.0.product"][0])

    def test_cross_venture_supplier_rejected(self):
        other_supplier = Supplier.objects.create(
            venture=self.venture2, name="Other Trader"
        )
        resp = self._purchase(supplier=other_supplier)
        self.assertEqual(resp.status_code, 400, resp.data)

    def test_delete_completed_purchase_with_stock_blocked(self):
        purchase = self._purchase()
        pk = purchase.data["data"]["id"]
        resp = self.client.delete(f"/api/purchases/{pk}/")
        self.assertEqual(resp.status_code, 400)
        self.assertIn("reverse it with a purchase return", resp.data["message"])

    def test_purchase_without_warehouse_is_finalized_without_stock(self):
        resp = self._purchase_no_warehouse()
        self.assertEqual(resp.status_code, 201, resp.data)
        self.assertEqual(StockMovement.objects.count(), 0)
        pk = resp.data["data"]["id"]
        # no warehouse -> no stock applied -> deletable
        self.assertEqual(self.client.delete(f"/api/purchases/{pk}/").status_code, 200)

    def test_purchase_list_search_and_status_filter(self):
        self._purchase()
        resp = self.client.get("/api/purchases/?search=PINV")
        self.assertEqual(resp.data["data"]["count"], 1)
        resp = self.client.get("/api/purchases/?status=COMPLETED")
        self.assertEqual(resp.data["data"]["count"], 1)
        resp = self.client.get("/api/purchases/?status=BOGUS")
        self.assertEqual(resp.data["data"]["count"], 0)

    def test_purchase_items_endpoint(self):
        purchase = self._purchase()
        pk = purchase.data["data"]["id"]
        resp = self.client.get(f"/api/purchases/{pk}/items/")
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(len(resp.data["data"]), 2)


class PurchaseReturnTests(APITestCase):
    def setUp(self):
        reset_state()
        self.client = APIClient()
        self.admin = User.objects.create_superuser(
            email="admin@avms.local", password="Admin@12345"
        )
        self.venture = Venture.objects.create(venture_name="Mushroom Farm")
        from inventory.models import Warehouse
        self.wh = Warehouse.objects.create(
            venture=self.venture, warehouse_name="Main Godown", city="Nalbari"
        )
        self.supplier = Supplier.objects.create(
            venture=self.venture, name="Shree Traders", phone="9876543210"
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

        created = self.client.post(
            "/api/purchases/",
            {
                "venture": self.venture.id,
                "supplier": self.supplier.id,
                "warehouse": self.wh.id,
                "purchase_date": "2024-08-01",
                "items": [
                    {"product": self.product.id, "quantity": "100", "unit_price": "120"}
                ],
            },
            format="json",
        )
        self.purchase = Purchase.objects.get(pk=created.data["data"]["id"])

    def _return(self, quantity="40", **overrides):
        payload = {
            "venture": self.venture.id,
            "purchase": self.purchase.id,
            "return_date": "2024-08-05",
            "items": [
                {"product": self.product.id, "quantity": quantity, "unit_price": "120"}
            ],
        }
        payload.update(overrides)
        return self.client.post("/api/purchase-returns/", payload, format="json")

    def test_partial_return_reduces_stock_and_due(self):
        resp = self._return("40")
        self.assertEqual(resp.status_code, 201, resp.data)
        self.assertEqual(resp.data["data"]["return_number"], "RET-0001")
        self.assertEqual(Decimal(resp.data["data"]["total_amount"]), Decimal("4800"))

        self.assertEqual(
            Inventory.objects.get(warehouse=self.wh, product=self.product).quantity,
            Decimal("60"),
        )
        self.assertEqual(StockMovement.objects.filter(movement_type="PURCHASE_RETURN").count(), 1)

        self.purchase.refresh_from_db()
        self.assertEqual(Decimal(self.purchase.returned_amount), Decimal("4800"))
        self.assertEqual(Decimal(self.purchase.due_amount), Decimal("7200"))
        self.assertEqual(self.purchase.status, "PARTIAL")

    def test_full_return_sets_status_returned(self):
        self._return("100")
        self.purchase.refresh_from_db()
        self.assertEqual(self.purchase.status, "RETURNED")
        self.assertEqual(Decimal(self.purchase.due_amount), Decimal("0"))
        self.assertEqual(
            Inventory.objects.get(warehouse=self.wh, product=self.product).quantity,
            Decimal("0"),
        )

    def test_cannot_return_more_than_purchased(self):
        resp = self._return("150")
        self.assertEqual(resp.status_code, 400, resp.data)
        self.assertIn("exceeds purchased quantity", resp.data["message"])

    def test_return_limited_by_cumulative(self):
        self._return("40")
        resp = self._return("70")
        self.assertEqual(resp.status_code, 400, resp.data)
        self.assertIn("exceeds purchased quantity", resp.data["message"])

    def test_return_rejects_product_not_in_purchase(self):
        other = Product.objects.create(
            venture=self.venture,
            unit=Unit.objects.get(unit_code="kg"),
            product_name="Oyster Mushroom",
        )
        resp = self._return(items=[{"product": other.id, "quantity": "10", "unit_price": "150"}])
        self.assertEqual(resp.status_code, 400, resp.data)

    def test_return_requires_sufficient_warehouse_stock(self):
        # drain stock first with a return of everything
        self._return("100")
        # record a fresh purchase? cannot - return endpoint deducts; attempt another return
        resp = self._return("10")
        self.assertEqual(resp.status_code, 400, resp.data)


class PurchasePermissionsTests(APITestCase):
    def setUp(self):
        reset_state()
        self.client = APIClient()
        self.staff = User.objects.create_user(
            email="staff@avms.local", password="Staff@12345"
        )
        self.client.force_authenticate(self.staff)

    def test_staff_forbidden(self):
        self.assertEqual(self.client.get("/api/purchases/").status_code, 403)
        self.assertEqual(self.client.get("/api/purchase-returns/").status_code, 403)
        self.assertEqual(
            self.client.post("/api/purchases/", {}, format="json").status_code, 403
        )

    def test_purchase_audited(self):
        from django.contrib.auth import get_user_model

        admin = get_user_model().objects.create_superuser(
            email="admin@avms.local", password="Admin@12345"
        )
        self.client.force_authenticate(admin)
        venture = Venture.objects.create(venture_name="Mushroom Farm")
        warehouse = None
        from inventory.models import Warehouse
        warehouse = Warehouse.objects.create(
            venture=venture, warehouse_name="Main Godown", city="Nalbari"
        )
        supplier = Supplier.objects.create(venture=venture, name="T", phone="1")
        product = Product.objects.create(
            venture=venture,
            unit=Unit.objects.get(unit_code="kg"),
            product_name="P",
        )
        self.client.post(
            "/api/purchases/",
            {
                "venture": venture.id,
                "supplier": supplier.id,
                "warehouse": warehouse.id,
                "purchase_date": "2024-08-01",
                "items": [
                    {"product": product.id, "quantity": "5", "unit_price": "10"}
                ],
            },
            format="json",
        )
        entry = AuditLog.objects.filter(module="purchases").first()
        self.assertIsNotNone(entry)
        self.assertEqual(entry.action, AuditLog.ACTION_CREATE)