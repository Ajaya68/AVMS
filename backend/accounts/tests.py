"""Tests for the AVMS custom user model and user manager."""

from django.contrib.auth import get_user_model
from django.test import TestCase

User = get_user_model()


class UserManagerTests(TestCase):
    def test_create_user(self):
        user = User.objects.create_user(
            email="staff@avms.local", password="plain-password"
        )
        self.assertFalse(user.is_staff)
        self.assertFalse(user.is_superuser)
        self.assertTrue(user.is_active)
        self.assertTrue(user.check_password("plain-password"))

    def test_create_user_requires_email(self):
        with self.assertRaises(ValueError):
            User.objects.create_user(email=None, password="x")

    def test_create_user_normalizes_email(self):
        user = User.objects.create_user(email="User@AVMS.local", password="x")
        # Django lowercases only the domain part of the address.
        self.assertEqual(user.email, "User@avms.local")

    def test_create_superuser(self):
        user = User.objects.create_superuser(email="root@avms.local", password="x")
        self.assertTrue(user.is_staff)
        self.assertTrue(user.is_superuser)

    def test_superuser_requires_staff(self):
        with self.assertRaises(ValueError):
            User.objects.create_superuser(
                email="bad@avms.local", password="x", is_staff=False
            )

    def test_username_field_is_email(self):
        self.assertEqual(User.USERNAME_FIELD, "email")

    def test_str_representation(self):
        user = User.objects.create_user(email="one@avms.local", password="x")
        self.assertEqual(str(user), "one@avms.local")

class PermissionModelTests(TestCase):
    """Role/permission model behaviour and the capability helpers."""

    def setUp(self):
        from accounts.models import Permission, Role

        self.view_perm = Permission.objects.create(
            code="customers.view", name="View customers", module="customers"
        )
        self.manage_perm = Permission.objects.create(
            code="customers.manage", name="Manage customers", module="customers"
        )
        self.dashboard_perm = Permission.objects.create(
            code="dashboard.view", name="View dashboard", module="dashboard"
        )
        self.role = Role.objects.create(code="SALES_STAFF", name="Sales Staff")
        self.role.permissions.set(
            [self.dashboard_perm, self.view_perm, self.manage_perm]
        )

    def test_has_permission_code(self):
        user = User.objects.create_user(email="sales@avms.local", password="x")
        user.roles.add(self.role)
        self.assertTrue(user.has_permission_code("customers.view"))
        self.assertTrue(user.has_permission_code("customers.manage"))
        self.assertFalse(user.has_permission_code("inventory.manage"))

    def test_permission_codes_collection(self):
        user = User.objects.create_user(email="sales2@avms.local", password="x")
        user.roles.add(self.role)
        codes = user.permission_codes()
        self.assertIn("customers.view", codes)
        self.assertIn("customers.manage", codes)
        self.assertNotIn("inventory.view", codes)

    def test_superuser_bypasses_permission_checks(self):
        user = User.objects.create_superuser(
            email="root@avms.local", password="x"
        )
        self.assertTrue(user.has_permission_code("anything.at.all"))

    def test_inactive_role_grant_is_ignored(self):
        self.role.is_active = False
        self.role.save()
        user = User.objects.create_user(email="sales3@avms.local", password="x")
        user.roles.add(self.role)
        self.assertFalse(user.has_permission_code("customers.view"))

    def test_role_code_property(self):
        user = User.objects.create_user(email="sales4@avms.local", password="x")
        user.roles.add(self.role)
        self.assertIn("SALES_STAFF", user.role_codes)


class AuthApiTests(TestCase):
    """End-to-end auth endpoints using the API client."""

    def setUp(self):
        from rest_framework.test import APIClient

        from accounts.models import Permission, Role

        self.client = APIClient()
        self.dashboard_perm = Permission.objects.create(
            code="dashboard.view", name="View dashboard", module="dashboard"
        )
        self.admin = User.objects.create_superuser(
            email="admin@avms.local", password="Admin@12345"
        )
        self.admin_role = Role.objects.create(code="ADMIN", name="Administrator")
        self.staff_role = Role.objects.create(
            code="SALES_STAFF", name="Sales Staff"
        )
        self.staff_role.permissions.set([self.dashboard_perm])
        self.admin.roles.add(self.admin_role)
        self.staff = User.objects.create_user(
            email="staff@avms.local", password="Staff@12345"
        )
        self.staff.roles.add(self.staff_role)

    def _login(self, email, password):
        return self.client.post(
            "/api/auth/login/", {"email": email, "password": password}, format="json"
        )

    def test_login_success(self):
        resp = self._login("admin@avms.local", "Admin@12345")
        self.assertEqual(resp.status_code, 200)
        body = resp.json()
        self.assertTrue(body["success"])
        data = body["data"]
        self.assertIn("access", data)
        self.assertIn("refresh", data)
        self.assertEqual(data["user"]["email"], "admin@avms.local")
        self.assertIn("ADMIN", data["user"]["role_codes"])

    def test_login_with_only_email_username_field(self):
        # The login uses the email field, not a separate username.
        resp = self._login("admin@avms.local", "Admin@12345")
        self.assertEqual(resp.status_code, 200)

    def test_login_wrong_password(self):
        resp = self._login("admin@avms.local", "wrong-password")
        self.assertEqual(resp.status_code, 401)

    def test_me_requires_token(self):
        resp = self.client.get("/api/auth/me/")
        self.assertEqual(resp.status_code, 401)

    def test_me_returns_profile_and_permissions(self):
        token = self._login("staff@avms.local", "Staff@12345").json()["data"]["access"]
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {token}")
        resp = self.client.get("/api/auth/me/")
        self.assertEqual(resp.status_code, 200)
        data = resp.json()["data"]
        self.assertEqual(data["email"], "staff@avms.local")
        self.assertIn("SALES_STAFF", data["role_codes"])

        permissions = data["permissions"]
        self.assertIn("dashboard.view", permissions)
        self.assertNotIn("users.manage", permissions)

    def test_admin_can_create_user(self):
        token = self._login("admin@avms.local", "Admin@12345").json()["data"]["access"]
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {token}")
        resp = self.client.post(
            "/api/auth/users/",
            {
                "email": "newuser@avms.local",
                "password": "SomePass123",
                "full_name": "New User",
                "roles": [self.staff_role.id],
            },
            format="json",
        )
        self.assertEqual(resp.status_code, 201)
        self.assertTrue(User.objects.filter(email="newuser@avms.local").exists())

    def test_non_admin_cannot_create_user(self):
        token = self._login("staff@avms.local", "Staff@12345").json()["data"]["access"]
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {token}")
        resp = self.client.post(
            "/api/auth/users/",
            {"email": "hacker@avms.local", "password": "SomePass123"},
            format="json",
        )
        self.assertEqual(resp.status_code, 403)

    def test_roles_list_requires_admin(self):
        token = self._login("staff@avms.local", "Staff@12345").json()["data"]["access"]
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {token}")
        resp = self.client.get("/api/auth/roles/")
        self.assertEqual(resp.status_code, 403)

    def test_roles_list(self):
        token = self._login("admin@avms.local", "Admin@12345").json()["data"]["access"]
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {token}")
        resp = self.client.get("/api/auth/roles/")
        self.assertEqual(resp.status_code, 200)
        data = resp.json()["data"]
        codes = [r["code"] for r in data]
        self.assertIn("ADMIN", codes)
        self.assertIn("SALES_STAFF", codes)

    def test_deactivate_user(self):
        token = self._login("admin@avms.local", "Admin@12345").json()["data"]["access"]
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {token}")
        resp = self.client.patch(
            f"/api/auth/users/{self.staff.id}/", {"is_active": False}, format="json"
        )
        self.assertEqual(resp.status_code, 200)
        user = User.objects.get(pk=self.staff.id)
        self.assertFalse(user.is_active)

    def test_logout_blacklists_refresh(self):
        login = self._login("staff@avms.local", "Staff@12345").json()["data"]
        token = login["access"]
        refresh = login["refresh"]
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {token}")
        resp = self.client.post(
            "/api/auth/logout/", {"refresh": refresh}, format="json"
        )
        self.assertEqual(resp.status_code, 200)

        # The blacklisted refresh must no longer work.
        resp = self.client.post(
            "/api/auth/refresh/", {"refresh": refresh}, format="json"
        )
        self.assertEqual(resp.status_code, 401)
