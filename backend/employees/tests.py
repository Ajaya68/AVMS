from decimal import Decimal

from django.contrib.auth import get_user_model
from rest_framework.test import APIClient, APITestCase

from audit.models import AuditLog
from core.models import SequenceCounter
from ventures.models import Venture, VentureCodeCounter

from .models import Employee

User = get_user_model()


def reset_state():
    SequenceCounter.objects.filter(module__in=["employees"]).delete()
    Employee.objects.all().delete()
    VentureCodeCounter.objects.all().delete()
    Venture.objects.all().delete()


class EmployeeTests(APITestCase):
    def setUp(self):
        reset_state()
        self.client = APIClient()
        self.admin = User.objects.create_superuser(
            email="admin@avms.local", password="Admin@12345"
        )
        self.venture = Venture.objects.create(venture_name="Mushroom Farm")
        self.venture2 = Venture.objects.create(venture_name="Fish Pond")
        self.client.force_authenticate(self.admin)

    def _employee(self, **overrides):
        payload = {
            "venture": self.venture.id,
            "first_name": "Amit",
            "last_name": "Sharma",
            "phone": "9876543210",
            "email": "amit@example.com",
            "department": "Production",
            "designation": "Supervisor",
            "joining_date": "2024-01-15",
            "salary": "25000",
        }
        payload.update(overrides)
        return self.client.post("/api/employees/", payload, format="json")

    def test_create_employee_auto_code(self):
        resp = self._employee()
        self.assertEqual(resp.status_code, 201, resp.data)
        self.assertEqual(resp.data["data"]["employee_code"], "EMP-0001")
        self.assertEqual(resp.data["data"]["name"], "Amit Sharma")
        self.assertEqual(resp.data["data"]["status"], "ACTIVE")

    def test_update_and_delete_employee(self):
        resp = self._employee()
        pk = resp.data["data"]["id"]
        resp = self.client.patch(
            f"/api/employees/{pk}/", {"salary": "30000", "status": "INACTIVE"}
        )
        self.assertEqual(resp.status_code, 200, resp.data)
        self.assertEqual(Decimal(resp.data["data"]["salary"]), Decimal("30000"))
        self.assertEqual(resp.data["data"]["status"], "INACTIVE")
        self.assertEqual(self.client.delete(f"/api/employees/{pk}/").status_code, 200)
        self.assertEqual(Employee.objects.count(), 0)

    def test_list_search_and_status_filter(self):
        self._employee()
        self._employee(first_name="Priya", department="Sales", status="INACTIVE")
        self.assertEqual(self.client.get("/api/employees/?search=priya").data["data"]["count"], 1)
        self.assertEqual(self.client.get("/api/employees/?status=INACTIVE").data["data"]["count"], 1)

    def test_codes_unique_per_venture(self):
        self._employee()
        self._employee(venture=self.venture2.id)
        self.assertEqual(Employee.objects.filter(employee_code="EMP-0001").count(), 2)

    def test_staff_forbidden(self):
        staff = User.objects.create_user(
            email="staff@avms.local", password="Staff@12345"
        )
        self.client.force_authenticate(staff)
        self.assertEqual(self.client.get("/api/employees/").status_code, 403)
        self.assertEqual(
            self.client.post("/api/employees/", {}, format="json").status_code, 403
        )

    def test_employee_audited(self):
        self._employee()
        entry = AuditLog.objects.filter(module="employees").first()
        self.assertIsNotNone(entry)
        self.assertEqual(entry.action, AuditLog.ACTION_CREATE)