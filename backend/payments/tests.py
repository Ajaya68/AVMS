from decimal import Decimal

from django.contrib.auth import get_user_model
from rest_framework.test import APIClient, APITestCase

from audit.models import AuditLog
from core.models import SequenceCounter
from customers.models import Customer
from inventory.models import Inventory, StockMovement, Warehouse
from products.models import Category, Product, Unit
from purchases.models import Purchase
from sales.models import Sale
from suppliers.models import Supplier
from ventures.models import Venture, VentureCodeCounter

from .models import Payment

User = get_user_model()


def reset_state():
    Payment.objects.all().delete()
    Purchase.objects.all().delete()
    Sale.objects.all().delete()
    Product.objects.all().delete()
    Category.objects.all().delete()
    Customer.objects.all().delete()
    Supplier.objects.all().delete()
    Inventory.objects.all().delete()
    StockMovement.objects.all().delete()
    SequenceCounter.objects.filter(
        module__in=["purchases", "sales", "warehouses"]
    ).delete()
    Warehouse.objects.all().delete()
    VentureCodeCounter.objects.all().delete()
    Venture.objects.all().delete()


class PaymentTests(APITestCase):
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
        self.supplier = Supplier.objects.create(
            venture=self.venture, name="Shree Traders", phone="1"
        )
        self.customer = Customer.objects.create(
            venture=self.venture, name="Ravi Stores", phone="2"
        )
        self.product = Product.objects.create(
            venture=self.venture,
            unit=Unit.objects.get(unit_code="kg"),
            product_name="Button Mushroom",
            purchase_price="120",
            selling_price="180",
        )
        self.client.force_authenticate(self.admin)

        # stock in, then create a sale (total 7400, paid 2000 -> due 5400)
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
        sale_resp = self.client.post(
            "/api/sales/",
            {
                "venture": self.venture.id,
                "customer": self.customer.id,
                "warehouse": self.wh.id,
                "sale_date": "2024-08-01",
                "paid_amount": "2000",
                "items": [
                    {"product": self.product.id, "quantity": "40", "unit_price": "180"}
                ],
            },
            format="json",
        )
        self.sale = Sale.objects.get(pk=sale_resp.data["data"]["id"])
        self.assertEqual(Decimal(self.sale.due_amount), Decimal("5200"))

        # purchase (total 12000, paid 0 -> due 12000)
        purchase_resp = self.client.post(
            "/api/purchases/",
            {
                "venture": self.venture.id,
                "supplier": self.supplier.id,
                "warehouse": self.wh.id,
                "purchase_date": "2024-08-02",
                "items": [
                    {"product": self.product.id, "quantity": "100", "unit_price": "120"}
                ],
            },
            format="json",
        )
        self.purchase = Purchase.objects.get(pk=purchase_resp.data["data"]["id"])
        self.assertEqual(Decimal(self.purchase.due_amount), Decimal("12000"))

    def _payment(self, ref_type, ref_id, amount, **overrides):
        payload = {
            "venture": self.venture.id,
            "payment_type": "RECEIVED" if ref_type == "SALE" else "PAID",
            "reference_type": ref_type,
            "reference_id": ref_id,
            "amount": amount,
            "payment_date": "2024-08-05",
            "payment_method": "UPI",
        }
        payload.update(overrides)
        return self.client.post("/api/payments/", payload, format="json")

    def test_received_payment_settles_sale_due(self):
        resp = self._payment("SALE", self.sale.id, "5200")
        self.assertEqual(resp.status_code, 201, resp.data)
        self.assertEqual(Decimal(resp.data["data"]["amount"]), Decimal("5200"))
        self.assertEqual(resp.data["data"]["reference_label"], "SINV-0001")
        self.sale.refresh_from_db()
        self.assertEqual(Decimal(self.sale.paid_amount), Decimal("7200"))
        self.assertEqual(Decimal(self.sale.due_amount), Decimal("0"))

    def test_paid_payment_settles_purchase_due(self):
        resp = self._payment("PURCHASE", self.purchase.id, "5000")
        self.assertEqual(resp.status_code, 201, resp.data)
        self.purchase.refresh_from_db()
        self.assertEqual(Decimal(self.purchase.paid_amount), Decimal("5000"))
        self.assertEqual(Decimal(self.purchase.due_amount), Decimal("7000"))

    def test_over_payment_rejected(self):
        resp = self._payment("SALE", self.sale.id, "99999")
        self.assertEqual(resp.status_code, 400, resp.data)
        self.assertIn("exceeds the outstanding balance", resp.data["message"])
        self.assertEqual(Payment.objects.count(), 0)
        self.sale.refresh_from_db()
        self.assertEqual(Decimal(self.sale.due_amount), Decimal("5200"))

    def test_payment_requires_matching_type(self):
        # RECEIVED on a purchase should be rejected by service (bill kind mismatch
        # is blocked because RECEIVED implies SALE via view-level type check)
        resp = self.client.post(
            "/api/payments/",
            {
                "venture": self.venture.id,
                "payment_type": "RECEIVED",
                "reference_type": "PURCHASE",
                "reference_id": self.purchase.id,
                "amount": "100",
                "payment_date": "2024-08-05",
                "payment_method": "CASH",
            },
            format="json",
        )
        self.assertEqual(resp.status_code, 400, resp.data)

    def test_payment_reversed_on_delete(self):
        self._payment("SALE", self.sale.id, "5200")
        self.sale.refresh_from_db()
        self.assertEqual(Decimal(self.sale.due_amount), Decimal("0"))
        pk = Payment.objects.get().id
        resp = self.client.delete(f"/api/payments/{pk}/")
        self.assertEqual(resp.status_code, 200, resp.data)
        self.sale.refresh_from_db()
        self.assertEqual(Decimal(self.sale.paid_amount), Decimal("2000"))
        self.assertEqual(Decimal(self.sale.due_amount), Decimal("5200"))
        self.assertEqual(Payment.objects.count(), 0)

    def test_payment_permissions(self):
        staff = User.objects.create_user(
            email="staff@avms.local", password="Staff@12345"
        )
        self.client.force_authenticate(staff)
        self.assertEqual(self.client.get("/api/payments/").status_code, 403)
        self.assertEqual(
            self.client.post("/api/payments/", {}, format="json").status_code, 403
        )

    def test_payment_audited(self):
        self._payment("SALE", self.sale.id, "100")
        entry = AuditLog.objects.filter(module="payments").first()
        self.assertIsNotNone(entry)