from datetime import date

from django.contrib.auth import get_user_model
from rest_framework.test import APIClient, APITestCase

from core.models import SequenceCounter
from customers.models import Customer
from inventory.models import Inventory, StockMovement, Warehouse
from inventory.services import record_stock_movement
from notifications.models import Notification
from notifications.services import notify
from products.models import Category, Product, Unit
from sales.models import Sale, SaleItem
from sales.services import finalize_sale
from ventures.models import Venture, VentureCodeCounter

User = get_user_model()


class NotificationTests(APITestCase):
    def setUp(self):
        Notification.objects.all().delete()
        SequenceCounter.objects.all().delete()
        VentureCodeCounter.objects.all().delete()
        for model in [
            Customer, Product, Category, Unit, StockMovement, Inventory,
            Warehouse, Sale, SaleItem, Venture,
        ]:
            model.objects.all().delete()

        self.admin = User.objects.create_superuser(
            email="admin@avms.local", password="Admin@12345"
        )
        self.other = User.objects.create_superuser(
            email="owner@avms.local", password="Owner@12345"
        )
        self.staff = User.objects.create_user(email="staff@avms.local", password="Staff@12345")
        self.venture = Venture.objects.create(venture_name="Mushroom Farm")

        self.client = APIClient()
        self.client.force_authenticate(self.admin)
        self.client.credentials(HTTP_X_VENTURE_ID=str(self.venture.id))

    def _setup_sku(self):
        unit = Unit.objects.create(unit_name="kg")
        category = Category.objects.create(venture=self.venture, category_name="Produce")
        self.product = Product.objects.create(
            venture=self.venture, category=category, unit=unit,
            product_name="Button Mushroom", purchase_price="100",
            selling_price="180", reorder_level="5",
        )
        self.warehouse = Warehouse.objects.create(
            venture=self.venture, warehouse_name="Main Store"
        )
        self.inventory = Inventory.objects.create(
            venture=self.venture, warehouse=self.warehouse,
            product=self.product, quantity="5", reorder_level="5",
        )

    def test_notify_and_own_only_listing(self):
        notify(self.admin, Notification.TYPE_SYSTEM, "Hello admin")
        notify(self.other, Notification.TYPE_SYSTEM, "Hello owner")

        data = self.client.get("/api/notifications/").data["data"]
        self.assertEqual(data["count"], 1)
        self.assertEqual(data["results"][0]["message"], "Hello admin")

        unread = self.client.get("/api/notifications/unread-count/").data["data"]
        self.assertEqual(unread["count"], 1)

    def test_low_stock_and_sale_notifications_flow(self):
        self._setup_sku()
        record_stock_movement(
            None,
            venture=self.venture, warehouse=self.warehouse, product=self.product,
            movement_type=StockMovement.MOVE_ADJUSTMENT_OUT,
            quantity=1, movement_date=date.today(),
        )
        self.assertTrue(Notification.objects.filter(
            user=self.admin, type=Notification.TYPE_LOW_STOCK, is_read=False
        ).exists())

        customer = Customer.objects.create(venture=self.venture, name="Ravi Stores")
        sale = Sale.objects.create(
            venture=self.venture, customer=customer, warehouse=self.warehouse,
            sale_date=date.today(),
        )
        finalize_sale(None, sale, self.warehouse, [SaleItem(
            sale=sale, product=self.product, quantity=1, unit_price="180"
        )])
        sale_note = Notification.objects.filter(
            user=self.admin, type=Notification.TYPE_SALE_CREATED
        ).first()
        self.assertIsNotNone(sale_note)

        sale_id = Notification.objects.filter(
            user=self.admin, type=Notification.TYPE_SALE_CREATED
        ).first().id
        self.assertEqual(
            self.client.post(f"/api/notifications/{sale_id}/read/").data["data"]["is_read"], True
        )
        self.client.post("/api/notifications/read-all/")
        self.assertEqual(
            Notification.objects.filter(user=self.admin, is_read=False).count(), 0
        )

    def test_staff_forbidden(self):
        client = APIClient()
        client.force_authenticate(self.staff)
        self.assertEqual(client.get("/api/notifications/").status_code, 403)
        self.assertEqual(client.post("/api/notifications/read-all/").status_code, 403)

    def test_notifications_newest_first(self):
        notify(self.admin, Notification.TYPE_SYSTEM, "older")
        notify(self.admin, Notification.TYPE_SYSTEM, "newer")
        resp = self.client.get("/api/notifications/?unread_first=false")
        results = resp.data["data"]["results"]
        self.assertEqual(results[0]["message"], "newer")