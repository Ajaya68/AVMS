from decimal import Decimal

from django.contrib.auth import get_user_model
from rest_framework.test import APIClient, APITestCase

from core.models import SequenceCounter
from customers.models import Customer
from expenses.models import Expense, ExpenseCategory
from inventory.models import Inventory, StockMovement, Warehouse
from payments.models import Payment
from products.models import Category, Product, Unit
from purchases.models import Purchase, PurchaseItem
from sales.models import Sale, SaleItem
from suppliers.models import Supplier
from ventures.models import Venture, VentureCodeCounter

User = get_user_model()


def reset_state():
    for model in [
        Sale, SaleItem, Purchase, PurchaseItem, Payment, Expense, ExpenseCategory,
        StockMovement, Inventory, Product, Warehouse, Customer, Supplier, Unit, Category,
        VentureCodeCounter, SequenceCounter, Venture,
    ]:
        model.objects.all().delete()


class ReportTests(APITestCase):
    def setUp(self):
        reset_state()
        self.client = APIClient()
        self.admin = User.objects.create_superuser(
            email="admin@avms.local", password="Admin@12345"
        )
        self.venture = Venture.objects.create(venture_name="Mushroom Farm")
        self.client.force_authenticate(self.admin)
        self.client.credentials(HTTP_X_VENTURE_ID=str(self.venture.id))

        unit = Unit.objects.create(unit_name="kg")
        category = Category.objects.create(venture=self.venture, category_name="Produce")
        self.product = Product.objects.create(
            venture=self.venture, category=category, unit=unit,
            product_name="Button Mushroom", purchase_price="100",
            selling_price="180", reorder_level="20",
        )
        self.customer = Customer.objects.create(venture=self.venture, name="Ravi Stores")
        self.supplier = Supplier.objects.create(venture=self.venture, name="Agro Traders")
        self.warehouse = Warehouse.objects.create(
            venture=self.venture, warehouse_name="Main Store"
        )
        self.sale = Sale.objects.create(
            venture=self.venture, customer=self.customer, warehouse=self.warehouse,
            sale_date="2026-09-10",
        )
        SaleItem.objects.create(
            sale=self.sale, product=self.product, quantity=10, unit_price="180", total="1800"
        )
        self.sale.subtotal = Decimal("1800")
        self.sale.total_amount = Decimal("1800")
        self.sale.due_amount = Decimal("0")
        self.sale.save()

        self.purchase = Purchase.objects.create(
            venture=self.venture, supplier=self.supplier, warehouse=self.warehouse,
            purchase_date="2026-09-11",
        )
        PurchaseItem.objects.create(
            purchase=self.purchase, product=self.product, quantity=50,
            unit_price="100", total="5000",
        )
        self.purchase.subtotal = Decimal("5000")
        self.purchase.total_amount = Decimal("5000")
        self.purchase.due_amount = Decimal("0")
        self.purchase.save()

        Inventory.objects.create(venture=self.venture, warehouse=self.warehouse, product=self.product, quantity="40")
        cat = ExpenseCategory.objects.create(category_name="Electricity", category_code="ELECTRICITY")
        Expense.objects.create(
            venture=self.venture, category=cat, amount="500", expense_date="2026-09-12"
        )
        Payment.objects.create(
            venture=self.venture, payment_type=Payment.TYPE_RECEIVED,
            reference_type=Payment.REF_SALE, reference_id=self.sale.id,
            amount="1800", payment_date="2026-09-12",
        )
        Payment.objects.create(
            venture=self.venture, payment_type=Payment.TYPE_PAID,
            reference_type=Payment.REF_PURCHASE, reference_id=self.purchase.id,
            amount="5000", payment_date="2026-09-12",
        )

    def test_sales_report(self):
        resp = self.client.get("/api/reports/sales/")
        self.assertEqual(resp.status_code, 200, resp.data)
        d = resp.data["data"]
        self.assertEqual(d["summary"]["count"], 1)
        self.assertEqual(Decimal(d["summary"]["total_amount"]), Decimal("1800"))
        self.assertEqual(Decimal(d["summary"]["net_amount"]), Decimal("1800"))
        self.assertEqual(d["top_products"][0]["product_name"], "Button Mushroom")
        self.assertEqual(d["series"][0]["label"], "2026-09-10")

    def test_purchases_report(self):
        resp = self.client.get("/api/reports/purchases/")
        self.assertEqual(resp.status_code, 200, resp.data)
        d = resp.data["data"]
        self.assertEqual(d["summary"]["count"], 1)
        self.assertEqual(Decimal(d["summary"]["total_amount"]), Decimal("5000"))
        self.assertEqual(d["top_suppliers"][0]["supplier_name"], "Agro Traders")

    def test_inventory_report(self):
        resp = self.client.get("/api/reports/inventory/")
        self.assertEqual(resp.status_code, 200, resp.data)
        d = resp.data["data"]
        self.assertEqual(Decimal(d["summary"]["total_quantity"]), Decimal("40"))
        self.assertEqual(Decimal(d["summary"]["stock_value"]), Decimal("4000"))
        self.assertEqual(d["items"][0]["product_code"], self.product.sku)

    def test_financial_report(self):
        resp = self.client.get("/api/reports/financial/")
        self.assertEqual(resp.status_code, 200, resp.data)
        d = resp.data["data"]
        self.assertEqual(Decimal(d["summary"]["revenue"]), Decimal("1800"))
        self.assertEqual(Decimal(d["summary"]["cogs"]), Decimal("1000"))
        self.assertEqual(Decimal(d["summary"]["expenses"]), Decimal("500"))
        self.assertEqual(Decimal(d["summary"]["gross_margin"]), Decimal("800"))
        self.assertEqual(Decimal(d["summary"]["net_profit"]), Decimal("300"))
        self.assertEqual(Decimal(d["outstanding"]["receivables"]), Decimal("1800"))
        self.assertEqual(Decimal(d["outstanding"]["payables"]), Decimal("5000"))
        self.assertEqual(Decimal(d["cash_flow"]["received"]), Decimal("1800"))
        self.assertEqual(Decimal(d["cash_flow"]["paid"]), Decimal("5000"))

    def test_report_scoped_to_venture(self):
        other = Venture.objects.create(venture_name="Fish Pond")
        self.client.credentials(HTTP_X_VENTURE_ID=str(other.id))
        d = self.client.get("/api/reports/sales/").data["data"]
        self.assertEqual(d["summary"]["count"], 0)

    def test_staff_forbidden(self):
        staff = User.objects.create_user(email="staff@avms.local", password="Staff@12345")
        self.client.force_authenticate(staff)
        self.assertEqual(self.client.get("/api/reports/sales/").status_code, 403)