from django.contrib.auth import get_user_model
from rest_framework.test import APIClient, APITestCase

from audit.models import AuditLog
from core.models import SequenceCounter
from ventures.models import Venture, VentureCodeCounter

from .models import Customer

User = get_user_model()


def reset_state():
    SequenceCounter.objects.filter(module="customers").delete()
    Customer.objects.all().delete()
    VentureCodeCounter.objects.all().delete()
    Venture.objects.all().delete()


class CustomerTests(APITestCase):
    def setUp(self):
        reset_state()
        self.client = APIClient()
        self.admin = User.objects.create_superuser(
            email="admin@avms.local", password="Admin@12345"
        )
        self.venture = Venture.objects.create(venture_name="Mushroom Farm")
        self.staff = User.objects.create_user(
            email="staff@avms.local", password="Staff@12345"
        )
        self.client.force_authenticate(self.admin)

    def _create(self, name="Ramesh"):
        return self.client.post(
            "/api/customers/",
            {"venture": self.venture.id, "name": name, "city": "Nalbari"},
            format="json",
        )

    def test_admin_can_create_with_auto_code(self):
        resp = self._create()
        self.assertEqual(resp.status_code, 201, resp.data)
        body = resp.data["data"]
        self.assertEqual(body["customer_code"], "C-0001")
        self.assertEqual(body["status"], "ACTIVE")
        second = self._create("Suresh").data["data"]
        self.assertEqual(second["customer_code"], "C-0002")

    def test_codes_never_repeat_after_delete(self):
        created = self._create().data["data"]
        self.client.delete(f"/api/customers/{created['id']}/")
        again = self._create("New").data["data"]
        self.assertEqual(again["customer_code"], "C-0002")

    def test_list_requires_customers_view(self):
        self._create()
        self.client.force_authenticate(self.staff)
        resp = self.client.get("/api/customers/")
        self.assertEqual(resp.status_code, 403)

    def test_create_requires_customers_manage(self):
        self.client.force_authenticate(self.staff)
        resp = self.client.post(
            "/api/customers/",
            {"venture": self.venture.id, "name": "X"},
            format="json",
        )
        self.assertEqual(resp.status_code, 403)

    def test_update_and_delete_audit(self):
        created = self._create().data["data"]
        resp = self.client.patch(
            f"/api/customers/{created['id']}/",
            {"status": "INACTIVE"},
            format="json",
        )
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(resp.data["data"]["status"], "INACTIVE")
        self.client.delete(f"/api/customers/{created['id']}/")

        actions = list(
            AuditLog.objects.filter(module="customers")
            .order_by("id")
            .values_list("action", flat=True)
        )
        self.assertEqual(actions, [AuditLog.ACTION_CREATE, AuditLog.ACTION_UPDATE, AuditLog.ACTION_DELETE])

    def test_search(self):
        self._create("Ramesh Das")
        self._create("Priya Boro")
        resp = self.client.get("/api/customers/?search=priya")
        self.assertEqual(resp.data["data"]["count"], 1)
        self.assertEqual(resp.data["data"]["results"][0]["name"], "Priya Boro")

    def test_venture_scoping_header(self):
        other = Venture.objects.create(venture_name="Fish Pond")
        self._create("A")
        self.client.post(
            "/api/customers/",
            {"venture": other.id, "name": "B"},
            format="json",
        )
        unscoped = self.client.get("/api/customers/")
        self.assertEqual(unscoped.data["data"]["count"], 2)

        resp = self.client.get(
            "/api/customers/", HTTP_X_VENTURE_ID=str(other.id)
        )
        self.assertEqual(resp.data["data"]["count"], 1)
        self.assertEqual(resp.data["data"]["results"][0]["name"], "B")