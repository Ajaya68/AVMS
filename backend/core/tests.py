"""Tests for the shared core utilities and health endpoints."""

from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase

from core.responses import failure, success


class ResponsesHelperTests(APITestCase):
    def test_success_envelope(self):
        response = success({"id": 1}, message="Created", status=201)
        self.assertEqual(response.status_code, 201)
        self.assertEqual(
            response.data,
            {"success": True, "data": {"id": 1}, "message": "Created"},
        )

    def test_success_defaults(self):
        response = success()
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data["success"], True)
        self.assertEqual(response.data["data"], None)

    def test_failure_envelope(self):
        response = failure(errors={"name": ["Required"]}, status=422)
        self.assertEqual(response.status_code, 422)
        self.assertEqual(response.data["success"], False)
        self.assertEqual(response.data["errors"]["name"], ["Required"])


class HealthViewTests(APITestCase):
    def test_health_endpoint_reports_ok(self):
        url = reverse("health")
        response = self.client.get(url)
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertTrue(response.data["success"])
        self.assertEqual(response.data["data"]["status"], "ok")
        self.assertEqual(response.data["data"]["database"], "ok")
        self.assertEqual(response.data["data"]["service"], "avms-backend")

    def test_health_short_alias(self):
        response = self.client.get(reverse("health-short"))
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["data"]["status"], "ok")

    def test_health_is_public(self):
        url = reverse("health")
        response = self.client.get(url)
        # 401 would be returned if authentication were enforced.
        self.assertNotEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)