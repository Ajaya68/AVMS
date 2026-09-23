from decimal import Decimal

from django.contrib.auth import get_user_model
from rest_framework.test import APIClient, APITestCase

from audit.models import AuditLog
from ventures.models import Venture

from .models import Expense, ExpenseCategory

User = get_user_model()


def reset_state():
    Expense.objects.all().delete()
    Venture.objects.all().delete()


class ExpenseTests(APITestCase):
    def setUp(self):
        reset_state()
        self.client = APIClient()
        self.admin = User.objects.create_superuser(
            email="admin@avms.local", password="Admin@12345"
        )
        self.venture = Venture.objects.create(venture_name="Mushroom Farm")
        self.electricity = ExpenseCategory.objects.get(category_code="ELECTRICITY")
        self.transport = ExpenseCategory.objects.get(category_code="TRANSPORT")
        self.client.force_authenticate(self.admin)

    def _expense(self, amount="5000", category=None, **overrides):
        payload = {
            "venture": self.venture.id,
            "category": (category or self.electricity).id,
            "amount": amount,
            "expense_date": "2024-08-01",
            "payment_method": "CASH",
            "description": "Power bill",
        }
        payload.update(overrides)
        return self.client.post("/api/expenses/", payload, format="json")

    def test_categories_seeded(self):
        resp = self.client.get("/api/expense-categories/")
        self.assertEqual(resp.status_code, 200)
        names = {c["category_name"] for c in resp.data["data"]}
        self.assertGreaterEqual(len(names), 8)
        self.assertIn("Electricity", names)

    def test_create_expense(self):
        resp = self._expense()
        self.assertEqual(resp.status_code, 201, resp.data)
        self.assertEqual(Decimal(resp.data["data"]["amount"]), Decimal("5000"))
        self.assertEqual(resp.data["data"]["category_name"], "Electricity")
        self.assertEqual(Expense.objects.count(), 1)

    def test_delete_expense(self):
        resp = self._expense()
        pk = resp.data["data"]["id"]
        self.assertEqual(self.client.delete(f"/api/expenses/{pk}/").status_code, 200)
        self.assertEqual(Expense.objects.count(), 0)

    def test_expense_list_search_and_category_filter(self):
        self._expense()
        self._expense(amount="2000", category=self.transport, description="Diesel")
        self.assertEqual(self.client.get("/api/expenses/?search=diesel").data["data"]["count"], 1)
        self.assertEqual(
            self.client.get(f"/api/expenses/?category={self.electricity.id}").data["data"]["count"],
            1,
        )

    def test_expense_permissions(self):
        staff = User.objects.create_user(
            email="staff@avms.local", password="Staff@12345"
        )
        self.client.force_authenticate(staff)
        self.assertEqual(self.client.get("/api/expenses/").status_code, 403)
        self.assertEqual(self.client.post("/api/expenses/", {}, format="json").status_code, 403)

    def test_expense_audited(self):
        self._expense()
        entry = AuditLog.objects.filter(module="expenses").first()
        self.assertIsNotNone(entry)
        self.assertEqual(entry.action, AuditLog.ACTION_CREATE)