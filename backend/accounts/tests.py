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