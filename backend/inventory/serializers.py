from rest_framework import serializers

from .models import Inventory, StockMovement, Warehouse
from .services import InsufficientStockError, record_stock_movement


class WarehouseSerializer(serializers.ModelSerializer):
    venture_name = serializers.CharField(source="venture.venture_code", read_only=True)

    class Meta:
        model = Warehouse
        fields = [
            "id",
            "venture",
            "venture_name",
            "warehouse_code",
            "warehouse_name",
            "address",
            "city",
            "state",
            "pincode",
            "manager",
            "status",
            "created_at",
            "updated_at",
        ]
        read_only_fields = ["warehouse_code", "created_at", "updated_at"]


class InventorySerializer(serializers.ModelSerializer):
    warehouse_code = serializers.CharField(source="warehouse.warehouse_code", read_only=True)
    warehouse_name = serializers.CharField(source="warehouse.warehouse_name", read_only=True)
    product_sku = serializers.CharField(source="product.sku", read_only=True)
    product_name = serializers.CharField(source="product.product_name", read_only=True)
    unit_code = serializers.CharField(source="product.unit.unit_code", read_only=True)
    available = serializers.DecimalField(max_digits=14, decimal_places=2, read_only=True)
    is_low = serializers.BooleanField(read_only=True)
    venture_name = serializers.CharField(source="venture.venture_code", read_only=True)

    class Meta:
        model = Inventory
        fields = [
            "id",
            "venture",
            "venture_name",
            "warehouse",
            "warehouse_code",
            "warehouse_name",
            "product",
            "product_sku",
            "product_name",
            "unit_code",
            "quantity",
            "reserved_quantity",
            "available",
            "reorder_level",
            "is_low",
        ]


class StockMovementSerializer(serializers.ModelSerializer):
    warehouse_code = serializers.CharField(source="warehouse.warehouse_code", read_only=True)
    warehouse_name = serializers.CharField(source="warehouse.warehouse_name", read_only=True)
    product_sku = serializers.CharField(source="product.sku", read_only=True)
    product_name = serializers.CharField(source="product.product_name", read_only=True)
    destination_code = serializers.CharField(
        source="destination_warehouse.warehouse_code", read_only=True
    )
    venture_name = serializers.CharField(source="venture.venture_code", read_only=True)
    movement_date = serializers.DateField(required=False)
    quantity = serializers.DecimalField(max_digits=14, decimal_places=2)

    class Meta:
        model = StockMovement
        fields = [
            "id",
            "venture",
            "venture_name",
            "warehouse",
            "warehouse_code",
            "warehouse_name",
            "destination_warehouse",
            "destination_code",
            "product",
            "product_sku",
            "product_name",
            "movement_type",
            "quantity",
            "movement_date",
            "notes",
            "reference_type",
            "reference_id",
            "created_by",
            "created_at",
        ]
        read_only_fields = ["created_at", "venture"]

    def create(self, validated_data):
        request = self.context.get("request")
        validated_data["venture"] = validated_data["warehouse"].venture
        try:
            movement, paired = record_stock_movement(
                request,
                venture=validated_data["venture"],
                warehouse=validated_data["warehouse"],
                product=validated_data["product"],
                movement_type=validated_data["movement_type"],
                quantity=validated_data["quantity"],
                movement_date=validated_data.get("movement_date"),
                notes=validated_data.get("notes", ""),
                reference_type=validated_data.get("reference_type", ""),
                reference_id=validated_data.get("reference_id", ""),
                destination_warehouse=validated_data.get("destination_warehouse"),
            )
        except InsufficientStockError as exc:
            raise serializers.ValidationError({"quantity": [str(exc)]}) from exc
        except ValueError as exc:
            raise serializers.ValidationError({"detail": [str(exc)]}) from exc
        self.context["paired"] = paired
        return movement