from decimal import Decimal

from django.contrib.auth import get_user_model
from rest_framework.test import APIClient, APITestCase

from core.models import SequenceCounter
from ventures.models import Venture, VentureCodeCounter

from .models import Category, Product, Unit

User = get_user_model()


def reset_state():
    SequenceCounter.objects.filter(module="products").delete()
    Product.objects.all().delete()
    Category.objects.all().delete()
    VentureCodeCounter.objects.all().delete()
    Venture.objects.all().delete()


class ProductTests(APITestCase):
    def setUp(self):
        reset_state()
        self.client = APIClient()
        self.admin = User.objects.create_superuser(
            email="admin@avms.local", password="Admin@12345"
        )
        self.venture = Venture.objects.create(venture_name="Mushroom Farm")
        self.category = Category.objects.create(
            venture=self.venture, category_name="Mushroom"
        )
        self.unit_kg = Unit.objects.get(unit_code="kg")
        self.client.force_authenticate(self.admin)

    def _payload(self, name="Button Mushroom"):
        return {
            "venture": self.venture.id,
            "category": self.category.id,
            "unit": self.unit_kg.id,
            "product_name": name,
            "purchase_price": "120.00",
            "selling_price": "180.00",
            "tax_rate": "5.00",
            "reorder_level": "10",
        }

    def test_units_seeded(self):
        resp = self.client.get("/api/units/")
        self.assertEqual(resp.status_code, 200)
        codes = {u["unit_code"] for u in resp.data["data"]["results"]}
        self.assertEqual(codes, {"kg", "g", "pcs", "packet", "litre", "box"})

    def test_create_product_with_auto_sku(self):
        resp = self.client.post("/api/products/", self._payload(), format="json")
        self.assertEqual(resp.status_code, 201, resp.data)
        prod = resp.data["data"]
        self.assertEqual(prod["sku"], "P-0001")
        self.assertEqual(Decimal(prod["selling_price"]), Decimal("180.00"))

        other_venture = Venture.objects.create(venture_name="Fish Pond")
        resp = self.client.post(
            "/api/products/",
            {**self._payload("Tilapia"), "venture": other_venture.id},
            format="json",
        )
        self.assertEqual(resp.data["data"]["sku"], "P-0001")

    def test_sku_unique_per_venture(self):
        self.client.post("/api/products/", self._payload(), format="json")
        resp = self.client.post(
            "/api/products/", {**self._payload("Second")}, format="json"
        )
        self.assertEqual(resp.status_code, 201)
        self.assertEqual(resp.data["data"]["sku"], "P-0002")

    def test_category_and_product_scoped_and_searched(self):
        self.client.post("/api/products/", self._payload(), format="json")
        cat = self.client.post(
            "/api/categories/",
            {"venture": self.venture.id, "category_name": "Exotic"},
            format="json",
        )
        self.assertEqual(cat.status_code, 201)

        resp = self.client.get("/api/products/?search=button")
        self.assertEqual(resp.data["data"]["count"], 1)

        other = Venture.objects.create(venture_name="Fish Pond")
        resp = self.client.get(
            "/api/products/", HTTP_X_VENTURE_ID=str(other.id)
        )
        self.assertEqual(resp.data["data"]["count"], 0)

    def test_product_permissions(self):
        staff = User.objects.create_user(email="staff@avms.local", password="Staff@12345")
        self.client.force_authenticate(staff)
        self.assertEqual(self.client.get("/api/products/").status_code, 403)
        self.assertEqual(
            self.client.post(
                "/api/products/", self._payload(), format="json"
            ).status_code,
            403,
        )

    def test_delete_product(self):
        created = self.client.post(
            "/api/products/", self._payload(), format="json"
        ).data["data"]
        resp = self.client.delete(f"/api/products/{created['id']}/")
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(Product.objects.count(), 0)