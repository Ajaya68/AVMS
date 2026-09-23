"""Tests for the Venture model, CRUD API, permissions and audit trail."""

from django.contrib.auth import get_user_model
from django.test import TestCase
from rest_framework.test import APIClient

from audit.models import AuditLog
from ventures.models import Venture, VentureCodeCounter

User = get_user_model()


def reset_venture_state():
    """Reset ventures and the code counter so codes always restart at V-0001."""
    VentureCodeCounter.objects.all().delete()
    Venture.objects.all().delete()


class VentureModelTests(TestCase):
    def setUp(self):
        reset_venture_state()

    def test_generated_code_sequence(self):
        first = Venture.objects.create(venture_name="Mushroom Farm")
        second = Venture.objects.create(venture_name="Fish Pond")
        self.assertEqual(first.venture_code, "V-0001")
        self.assertEqual(second.venture_code, "V-0002")

    def test_default_status_and_business_type(self):
        venture = Venture.objects.create(venture_name="Agri Field")
        self.assertEqual(venture.status, Venture.Status.ACTIVE)
        self.assertEqual(venture.business_type, Venture.BusinessType.GENERAL)

    def test_str_representation(self):
        venture = Venture.objects.create(venture_name="Poultry Shed")
        self.assertEqual(str(venture), f"{venture.venture_code} - Poultry Shed")


class VentureApiTests(TestCase):
    def setUp(self):
        from accounts.models import Permission, Role

        self.client = APIClient()
        reset_venture_state()
        self.admin = User.objects.create_superuser(
            email="admin@avms.local", password="Admin@12345"
        )
        admin_role = Role.objects.create(code="ADMIN", name="Administrator")
        self.admin.roles.add(admin_role)

        manager_role = Role.objects.create(code="MANAGER", name="Manager")
        Permission.objects.create(
            code="ventures.view", name="View ventures", module="ventures"
        )
        Permission.objects.create(
            code="ventures.manage", name="Manage ventures", module="ventures"
        )
        manager_role.permissions.set(
            [
                Permission.objects.get(code="ventures.view"),
                Permission.objects.get(code="ventures.manage"),
            ]
        )
        self.manager = User.objects.create_user(
            email="manager@avms.local", password="Manager@12345"
        )
        self.manager.roles.add(manager_role)

        viewer_role = Role.objects.create(code="EMPLOYEE", name="Employee")
        viewer_role.permissions.set([Permission.objects.get(code="ventures.view")])
        self.viewer = User.objects.create_user(
            email="viewer@avms.local", password="Viewer@12345"
        )
        self.viewer.roles.add(viewer_role)

    def _auth(self, user):
        self.client.force_authenticate(user=user)

    def test_list_requires_authentication(self):
        resp = self.client.get("/api/ventures/")
        self.assertEqual(resp.status_code, 401)

    def test_viewer_can_list_but_not_create(self):
        Venture.objects.create(venture_name="Mushroom")
        self._auth(self.viewer)
        resp = self.client.get("/api/ventures/")
        self.assertEqual(resp.status_code, 200)
        self.assertTrue(resp.json()["success"])

        resp = self.client.post(
            "/api/ventures/", {"venture_name": "Hacker Farm"}, format="json"
        )
        self.assertEqual(resp.status_code, 403)

    def test_manager_can_create_venture(self):
        self._auth(self.manager)
        resp = self.client.post(
            "/api/ventures/",
            {
                "venture_name": "Mushroom Farm",
                "business_type": "MUSHROOM",
                "city": "Nalbari",
            },
            format="json",
        )
        self.assertEqual(resp.status_code, 201)
        data = resp.json()["data"]
        self.assertEqual(data["venture_code"], "V-0001")
        self.assertEqual(data["business_type"], "MUSHROOM")
        self.assertTrue(Venture.objects.filter(venture_name="Mushroom Farm").exists())

    def test_admin_can_update_venture(self):
        venture = Venture.objects.create(venture_name="Fish Pond", city="Guwahati")
        self._auth(self.admin)
        resp = self.client.patch(
            f"/api/ventures/{venture.id}/",
            {"venture_name": "Fish Pond II", "status": "INACTIVE"},
            format="json",
        )
        self.assertEqual(resp.status_code, 200)
        venture.refresh_from_db()
        self.assertEqual(venture.venture_name, "Fish Pond II")
        self.assertEqual(venture.status, Venture.Status.INACTIVE)

    def test_admin_can_delete_venture(self):
        venture = Venture.objects.create(venture_name="Temp")
        self._auth(self.admin)
        resp = self.client.delete(f"/api/ventures/{venture.id}/")
        self.assertEqual(resp.status_code, 200)
        self.assertFalse(Venture.objects.filter(pk=venture.id).exists())

    def test_detail_not_found(self):
        self._auth(self.admin)
        resp = self.client.get("/api/ventures/99999/")
        self.assertEqual(resp.status_code, 404)

    def test_search_by_name_and_code(self):
        Venture.objects.create(venture_name="Mushroom Farm")
        Venture.objects.create(venture_name="Fish Farm")
        self._auth(self.viewer)

        resp = self.client.get("/api/ventures/", {"search": "mushroom"})
        results = resp.json()["data"]["results"]
        self.assertEqual(len(results), 1)
        self.assertEqual(results[0]["venture_name"], "Mushroom Farm")

        resp = self.client.get("/api/ventures/", {"search": "V-0002"})
        results = resp.json()["data"]["results"]
        self.assertEqual(len(results), 1)
        self.assertEqual(results[0]["venture_name"], "Fish Farm")

    def test_status_filter(self):
        Venture.objects.create(venture_name="Active One")
        Venture.objects.create(venture_name="Closed One", status="INACTIVE")
        self._auth(self.viewer)

        resp = self.client.get("/api/ventures/", {"status": "INACTIVE"})
        results = resp.json()["data"]["results"]
        self.assertEqual(len(results), 1)
        self.assertEqual(results[0]["venture_name"], "Closed One")

    def test_duplicate_venture_name_allowed_but_code_unique(self):
        Venture.objects.create(venture_name="Same Name")
        Venture.objects.create(venture_name="Same Name")
        self.assertEqual(Venture.objects.filter(venture_name="Same Name").count(), 2)
        codes = list(Venture.objects.values_list("venture_code", flat=True))
        self.assertEqual(len(codes), len(set(codes)))

    def test_audit_logged_on_create_update_delete(self):
        self._auth(self.manager)
        resp = self.client.post(
            "/api/ventures/", {"venture_name": "Agri"}, format="json"
        )
        venture_id = resp.json()["data"]["id"]
        self.assertEqual(
            AuditLog.objects.filter(
                action=AuditLog.ACTION_CREATE, module="ventures"
            ).count(),
            1,
        )

        self.client.patch(
            f"/api/ventures/{venture_id}/", {"venture_name": "Agri Crop"}, format="json"
        )
        self.assertEqual(
            AuditLog.objects.filter(
                action=AuditLog.ACTION_UPDATE, module="ventures"
            ).count(),
            1,
        )

        self.client.delete(f"/api/ventures/{venture_id}/")
        self.assertEqual(
            AuditLog.objects.filter(
                action=AuditLog.ACTION_DELETE, module="ventures"
            ).count(),
            1,
        )