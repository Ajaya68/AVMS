from django.contrib.auth import get_user_model
from rest_framework.test import APIClient, APITestCase

from core.models import SequenceCounter
from ventures.models import Venture, VentureCodeCounter

from .models import Supplier

User = get_user_model()


def reset_state():
    SequenceCounter.objects.filter(module="suppliers").delete()
    Supplier.objects.all().delete()
    VentureCodeCounter.objects.all().delete()
    Venture.objects.all().delete()


class SupplierTests(APITestCase):
    def setUp(self):
        reset_state()
        self.client = APIClient()
        self.admin = User.objects.create_superuser(
            email="admin@avms.local", password="Admin@12345"
        )
        self.venture = Venture.objects.create(venture_name="Mushroom Farm")
        self.client.force_authenticate(self.admin)

    def test_admin_crud_with_auto_code(self):
        resp = self.client.post(
            "/api/suppliers/",
            {"venture": self.venture.id, "name": "Agro Traders", "city": "Barpeta"},
            format="json",
        )
        self.assertEqual(resp.status_code, 201, resp.data)
        sup = resp.data["data"]
        self.assertEqual(sup["supplier_code"], "S-0001")

        resp = self.client.patch(
            f"/api/suppliers/{sup['id']}/",
            {"contact_person": "Dipak"},
            format="json",
        )
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(resp.data["data"]["contact_person"], "Dipak")

        resp = self.client.delete(f"/api/suppliers/{sup['id']}/")
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(Supplier.objects.count(), 0)

    def test_permissions_enforced(self):
        staff = User.objects.create_user(email="staff@avms.local", password="Staff@12345")
        self.client.force_authenticate(staff)
        self.assertEqual(self.client.get("/api/suppliers/").status_code, 403)
        self.assertEqual(
            self.client.post(
                "/api/suppliers/",
                {"venture": self.venture.id, "name": "X"},
                format="json",
            ).status_code,
            403,
        )