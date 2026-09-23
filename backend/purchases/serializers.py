from decimal import Decimal

from rest_framework import serializers

from .models import Purchase, PurchaseItem, PurchaseReturn, PurchaseReturnItem
from .services import finalize_purchase, finalize_purchase_return


class PurchaseItemSerializer(serializers.ModelSerializer):
    product_sku = serializers.CharField(source="product.sku", read_only=True)
    product_name = serializers.CharField(source="product.product_name", read_only=True)
    unit_code = serializers.CharField(source="product.unit.unit_code", read_only=True)

    class Meta:
        model = PurchaseItem
        fields = [
            "id", "product", "product_sku", "product_name", "unit_code",
            "quantity", "unit_price", "discount", "tax", "total",
        ]
        read_only_fields = ["total"]


class PurchaseSerializer(serializers.ModelSerializer):
    supplier_name = serializers.CharField(source="supplier.name", read_only=True)
    supplier_code = serializers.CharField(source="supplier.supplier_code", read_only=True)
    venture_name = serializers.CharField(source="venture.venture_code", read_only=True)
    warehouse_code = serializers.CharField(source="warehouse.warehouse_code", read_only=True)
    items = PurchaseItemSerializer(many=True, required=False)

    class Meta:
        model = Purchase
        fields = [
            "id", "venture", "venture_name", "supplier", "supplier_code",
            "supplier_name", "warehouse", "warehouse_code", "invoice_number",
            "purchase_date", "status", "subtotal", "discount", "tax",
            "total_amount", "paid_amount", "returned_amount", "due_amount",
            "notes", "created_by", "items", "created_at", "updated_at",
        ]
        read_only_fields = [
            "invoice_number", "status", "subtotal", "total_amount",
            "returned_amount", "due_amount", "created_by", "created_at",
            "updated_at",
        ]

    def validate(self, attrs):
        venture = attrs.get("venture") or getattr(getattr(self, "instance", None), "venture", None)
        supplier = attrs.get("supplier", getattr(getattr(self, "instance", None), "supplier", None))
        warehouse = attrs.get("warehouse", getattr(getattr(self, "instance", None), "warehouse", None))
        items = attrs.get("items", [])
        for name, value, kind in [("supplier", supplier, "Supplier"), ("warehouse", warehouse, "Warehouse")]:
            if value is not None and value.venture_id != venture.id:
                raise serializers.ValidationError(
                    {name: [f"{kind} does not belong to the selected venture"]}
                )
        for i, item in enumerate(items):
            if not (Decimal(str(item["quantity"])) > 0):
                raise serializers.ValidationError({f"items.{i}": ["Quantity must be greater than zero"]})
            if item["product"].venture_id != venture.id:
                raise serializers.ValidationError(
                    {f"items.{i}.product": ["Product does not belong to the selected venture"]}
                )
        return attrs

    def create(self, validated_data):
        items = validated_data.pop("items", [])
        request = self.context.get("request")
        user = getattr(request, "user", None)
        if user is not None and getattr(user, "is_authenticated", False):
            validated_data["created_by"] = user

        purchase = Purchase.objects.create(**validated_data)

        purchase_items = [
            PurchaseItem(purchase=purchase, **fields) for fields in items
        ]
        finalize_purchase(
            request, purchase, purchase.warehouse, purchase_items
        )
        return purchase


class PurchaseReturnItemSerializer(serializers.ModelSerializer):
    product_sku = serializers.CharField(source="product.sku", read_only=True)
    product_name = serializers.CharField(source="product.product_name", read_only=True)

    class Meta:
        model = PurchaseReturnItem
        fields = [
            "id", "product", "product_sku", "product_name",
            "quantity", "unit_price", "total",
        ]
        read_only_fields = ["total"]


class PurchaseReturnSerializer(serializers.ModelSerializer):
    purchase_invoice = serializers.CharField(source="purchase.invoice_number", read_only=True)
    supplier_name = serializers.CharField(source="purchase.supplier.name", read_only=True)
    venture_name = serializers.CharField(source="venture.venture_code", read_only=True)
    items = PurchaseReturnItemSerializer(many=True, required=False)

    class Meta:
        model = PurchaseReturn
        fields = [
            "id", "venture", "venture_name", "purchase", "purchase_invoice",
            "supplier_name", "return_number", "return_date", "status",
            "total_amount", "notes", "created_by", "items", "created_at",
        ]
        read_only_fields = ["return_number", "status", "total_amount", "created_by", "created_at"]

    def validate(self, attrs):
        venture = attrs.get("venture") or getattr(getattr(self, "instance", None), "venture", None)
        purchase = attrs.get("purchase", getattr(getattr(self, "instance", None), "purchase", None))
        if purchase is not None and purchase.venture_id != venture.id:
            raise serializers.ValidationError(
                {"purchase": ["Purchase does not belong to the selected venture"]}
            )
        items = attrs.get("items", [])
        if purchase is not None:
            returnable_ids = set(
                purchase.items.values_list("product_id", flat=True)
            )
            for i, item in enumerate(items):
                if item["product"].venture_id != venture.id:
                    raise serializers.ValidationError(
                        {f"items.{i}.product": ["Product does not belong to the selected venture"]}
                    )
                if item["product"].id not in returnable_ids:
                    raise serializers.ValidationError(
                        {f"items.{i}.product": ["Product was not part of this purchase"]}
                    )
        return attrs

    def create(self, validated_data):
        items = validated_data.pop("items", [])
        request = self.context.get("request")
        user = getattr(request, "user", None)
        if user is not None and getattr(user, "is_authenticated", False):
            validated_data["created_by"] = user

        purchase = validated_data["purchase"]
        return_record = PurchaseReturn.objects.create(**validated_data)

        return_items = [
            PurchaseReturnItem(purchase_return=return_record, **fields)
            for fields in items
        ]
        finalize_purchase_return(request, return_record, return_items)
        return return_record