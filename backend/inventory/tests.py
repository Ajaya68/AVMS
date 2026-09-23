from decimal import Decimal

from django.contrib.auth import get_user_model
from rest_framework.test import APIClient, APITestCase

from audit.models import AuditLog
from core.models import SequenceCounter
from products.models import Category, Product, Unit
from ventures.models import Venture, VentureCodeCounter

from .models import Inventory, StockMovement, Warehouse

User = get_user_model()


def reset_state():
    SequenceCounter.objects.filter(module__in=["warehouses"]).delete()
    StockMovement.objects.all().delete()
    Inventory.objects.all().delete()
    Warehouse.objects.all().delete()
    Product.objects.all().delete()
    Category.objects.all().delete()
    VentureCodeCounter.objects.all().delete()
    Venture.objects.all().delete()


class InventoryTests(APITestCase):
    def setUp(self):
        reset_state()
        self.client = APIClient()
        self.admin = User.objects.create_superuser(
            email="admin@avms.local", password="Admin@12345"
        )
        self.venture = Venture.objects.create(venture_name="Mushroom Farm")
        self.wh1 = Warehouse.objects.create(
            venture=self.venture, warehouse_name="Main Godown", city="Nalbari"
        )
        self.wh2 = Warehouse.objects.create(
            venture=self.venture, warehouse_name="Cold Store", city="Guwahati"
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

    def _movement(self, payload):
        return self.client.post("/api/stock-movements/", payload, format="json")

    def test_warehouse_crud_auto_code(self):
        resp = self.client.post(
            "/api/warehouses/",
            {"venture": self.venture.id, "warehouse_name": "New Store", "city": "Barpeta"},
            format="json",
        )
        self.assertEqual(resp.status_code, 201, resp.data)
        self.assertEqual(resp.data["data"]["warehouse_code"], "W-0003")
        self.assertEqual(Warehouse.objects.count(), 3)

    def test_purchase_increases_and_sale_decreases_stock(self):
        resp = self._movement({
            "warehouse": self.wh1.id,
            "product": self.product.id,
            "movement_type": "PURCHASE",
            "quantity": "100",
        })
        self.assertEqual(resp.status_code, 201, resp.data)
        self.assertEqual(resp.data["message"], "Stock movement recorded")

        resp = self._movement({
            "warehouse": self.wh1.id,
            "product": self.product.id,
            "movement_type": "SALE",
            "quantity": "30",
        })
        self.assertEqual(resp.status_code, 201)

        inv = Inventory.objects.get(warehouse=self.wh1, product=self.product)
        self.assertEqual(inv.quantity, Decimal("70"))
        self.assertEqual(inv.available, Decimal("70"))
        self.assertEqual(StockMovement.objects.count(), 2)

    def test_sale_rejects_insufficient_stock(self):
        resp = self._movement({
            "warehouse": self.wh1.id,
            "product": self.product.id,
            "movement_type": "SALE",
            "quantity": "10",
        })
        self.assertEqual(resp.status_code, 400)
        self.assertIn("Insufficient stock", resp.data["errors"]["quantity"][0])

    def test_adjustment_and_sales_return(self):
        self._movement({
            "warehouse": self.wh1.id,
            "product": self.product.id,
            "movement_type": "PURCHASE",
            "quantity": "50",
        })
        self._movement({
            "warehouse": self.wh1.id,
            "product": self.product.id,
            "movement_type": "ADJUSTMENT_IN",
            "quantity": "10",
        })
        self._movement({
            "warehouse": self.wh1.id,
            "product": self.product.id,
            "movement_type": "ADJUSTMENT_OUT",
            "quantity": "5",
        })
        inv = Inventory.objects.get(warehouse=self.wh1, product=self.product)
        self.assertEqual(inv.quantity, Decimal("55"))

    def test_transfer_moves_stock_between_warehouses(self):
        resp = self._movement({
            "warehouse": self.wh1.id,
            "destination_warehouse": self.wh2.id,
            "product": self.product.id,
            "movement_type": "TRANSFER_OUT",
            "quantity": "40",
        })
        self.assertEqual(resp.status_code, 400)  # nothing to transfer

        self._movement({
            "warehouse": self.wh1.id,
            "product": self.product.id,
            "movement_type": "PURCHASE",
            "quantity": "100",
        })
        resp = self._movement({
            "warehouse": self.wh1.id,
            "destination_warehouse": self.wh2.id,
            "product": self.product.id,
            "movement_type": "TRANSFER_OUT",
            "quantity": "40",
        })
        self.assertEqual(resp.status_code, 201, resp.data)

        self.assertEqual(
            Inventory.objects.get(warehouse=self.wh1, product=self.product).quantity,
            Decimal("60"),
        )
        self.assertEqual(
            Inventory.objects.get(warehouse=self.wh2, product=self.product).quantity,
            Decimal("40"),
        )
        types = set(StockMovement.objects.values_list("movement_type", flat=True))
        self.assertEqual(types, {"PURCHASE", "TRANSFER_OUT", "TRANSFER_IN"})

    def test_transfer_requires_destination(self):
        self._movement({
            "warehouse": self.wh1.id,
            "product": self.product.id,
            "movement_type": "PURCHASE",
            "quantity": "10",
        })
        resp = self._movement({
            "warehouse": self.wh1.id,
            "product": self.product.id,
            "movement_type": "TRANSFER_OUT",
            "quantity": "5",
        })
        self.assertEqual(resp.status_code, 400)

    def test_inventory_low_stock_filter(self):
        self._movement({
            "warehouse": self.wh1.id,
            "product": self.product.id,
            "movement_type": "PURCHASE",
            "quantity": "2",
        })
        resp = self.client.get("/api/inventory/?low=true")
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(resp.data["data"]["count"], 1)
        self.assertTrue(resp.data["data"]["results"][0]["is_low"])

        self._movement({
            "warehouse": self.wh1.id,
            "product": self.product.id,
            "movement_type": "PURCHASE",
            "quantity": "100",
        })
        resp = self.client.get("/api/inventory/?low=true")
        self.assertEqual(resp.data["data"]["count"], 0)

    def test_inventory_and_movement_permissions(self):
        staff = User.objects.create_user(
            email="staff@avms.local", password="Staff@12345"
        )
        self.client.force_authenticate(staff)
        self.assertEqual(self.client.get("/api/inventory/").status_code, 403)
        self.assertEqual(self.client.get("/api/stock-movements/").status_code, 403)
        self.assertEqual(self.client.get("/api/warehouses/").status_code, 403)

    def test_movement_audited(self):
        self._movement({
            "warehouse": self.wh1.id,
            "product": self.product.id,
            "movement_type": "PURCHASE",
            "quantity": "25",
        })
        entry = AuditLog.objects.filter(module="stock_movements").first()
        self.assertIsNotNone(entry)
        self.assertEqual(entry.action, AuditLog.ACTION_CREATE)

    def test_cannot_mix_ventures(self):
        other = Venture.objects.create(venture_name="Fish Pond")
        other_product = Product.objects.create(
            venture=other,
            unit=Unit.objects.get(unit_code="kg"),
            product_name="Tilapia",
        )
        resp = self._movement({
            "warehouse": self.wh1.id,
            "product": other_product.id,
            "movement_type": "PURCHASE",
            "quantity": "10",
        })
        self.assertEqual(resp.status_code, 400)
        self.assertEqual(StockMovement.objects.count(), 0)