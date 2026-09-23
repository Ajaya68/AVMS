from rest_framework import serializers

from .models import Category, Product, Unit


class UnitSerializer(serializers.ModelSerializer):
    class Meta:
        model = Unit
        fields = ["id", "unit_code", "unit_name", "is_base"]


class CategorySerializer(serializers.ModelSerializer):
    venture_name = serializers.CharField(source="venture.venture_code", read_only=True)

    class Meta:
        model = Category
        fields = [
            "id",
            "venture",
            "venture_name",
            "category_name",
            "description",
            "status",
            "created_at",
            "updated_at",
        ]
        read_only_fields = ["created_at", "updated_at"]


class ProductSerializer(serializers.ModelSerializer):
    venture_name = serializers.CharField(source="venture.venture_code", read_only=True)
    category_name = serializers.CharField(source="category.category_name", read_only=True)
    unit_code = serializers.CharField(source="unit.unit_code", read_only=True)

    class Meta:
        model = Product
        fields = [
            "id",
            "venture",
            "venture_name",
            "category",
            "category_name",
            "unit",
            "unit_code",
            "sku",
            "product_name",
            "description",
            "purchase_price",
            "selling_price",
            "tax_rate",
            "reorder_level",
            "status",
            "created_at",
            "updated_at",
        ]
        read_only_fields = ["sku", "created_at", "updated_at"]