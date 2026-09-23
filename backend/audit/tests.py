from django.contrib.auth import get_user_model
from rest_framework.test import APIClient, APITestCase

from audit.models import AuditLog
from core.models import SequenceCounter
from customers.models import Customer
from ventures.models import Venture, VentureCodeCounter

User = get_user_model()


class AuditLogTests(APITestCase):
    def setUp(self):
        SequenceCounter.objects.all().delete()
        VentureCodeCounter.objects.all().delete()
        Venture.objects.all().delete()
        Customer.objects.all().delete()
        AuditLog.objects.all().delete()

        self.admin = User.objects.create_superuser(
            email="admin@avms.local", password="Admin@12345"
        )
        self.staff = User.objects.create_user(email="staff@avms.local", password="Staff@12345")
        self.venture = Venture.objects.create(venture_name="Mushroom Farm")
        self.client = APIClient()
        self.client.force_authenticate(self.admin)
        self.client.credentials(HTTP_X_VENTURE_ID=str(self.venture.id))

    def test_list_and_filters(self):
        self.client.post(
            "/api/customers/",
            {"venture": self.venture.id, "name": "Ravi Stores", "city": "Nainital"},
            format="json",
        )
        data = self.client.get("/api/audit-logs/").data["data"]
        self.assertEqual(data["count"], 1, data)
        entry = data["results"][0]
        self.assertEqual(entry["module"], "customers")
        self.assertEqual(entry["action"], AuditLog.ACTION_CREATE)
        self.assertEqual(entry["user_email"], "admin@avms.local")

        by_module = self.client.get("/api/audit-logs/?module=customers").data["data"]
        self.assertEqual(by_module["count"], 1)
        by_action = self.client.get("/api/audit-logs/?module=customers&action=CREATE").data["data"]
        self.assertEqual(by_action["count"], 1)
        by_search = self.client.get("/api/audit-logs/?search=ravi").data["data"]
        self.assertEqual(by_search["count"], 1)
        wrong = self.client.get("/api/audit-logs/?module=sales").data["data"]
        self.assertEqual(wrong["count"], 0)

    def test_staff_forbidden(self):
        client = APIClient()
        client.force_authenticate(self.staff)
        self.assertEqual(client.get("/api/audit-logs/").status_code, 403)