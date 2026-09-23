from decimal import Decimal

from rest_framework import serializers

from .models import Sale, SaleItem, SaleReturn, SaleReturnItem
from .services import finalize_sale, finalize_sale_return


class SaleItemSerializer(serializers.ModelSerializer):
    product_sku = serializers.CharField(source="product.sku", read_only=True)
    product_name = serializers.CharField(source="product.product_name", read_only=True)
    unit_code = serializers.CharField(source="product.unit.unit_code", read_only=True)

    class Meta:
        model = SaleItem
        fields = [
            "id", "product", "product_sku", "product_name", "unit_code",
            "quantity", "unit_price", "discount", "tax", "total",
        ]
        read_only_fields = ["total"]


class SaleSerializer(serializers.ModelSerializer):
    customer_name = serializers.CharField(source="customer.name", read_only=True)
    customer_code = serializers.CharField(source="customer.customer_code", read_only=True)
    venture_name = serializers.CharField(source="venture.venture_code", read_only=True)
    warehouse_code = serializers.CharField(source="warehouse.warehouse_code", read_only=True)
    items = SaleItemSerializer(many=True, required=False)

    class Meta:
        model = Sale
        fields = [
            "id", "venture", "venture_name", "customer", "customer_code",
            "customer_name", "warehouse", "warehouse_code", "invoice_number",
            "sale_date", "status", "subtotal", "discount", "tax",
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
        customer = attrs.get("customer", getattr(getattr(self, "instance", None), "customer", None))
        warehouse = attrs.get("warehouse", getattr(getattr(self, "instance", None), "warehouse", None))
        items = attrs.get("items", [])
        for name, value, kind in [("customer", customer, "Customer"), ("warehouse", warehouse, "Warehouse")]:
            if value is not None and value.venture_id != venture.id:
                raise serializers.ValidationError(
                    {name: [f"{kind} does not belong to the selected venture"]}
                )
        for i, item in enumerate(items):
            if not (Decimal(str(item["quantity"])) > 0):
                raise serializers.ValidationError(
                    {f"items.{i}": ["Quantity must be greater than zero"]}
                )
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

        sale = Sale(**validated_data)
        sale_items = [SaleItem(sale=sale, **fields) for fields in items]
        finalize_sale(request, sale, sale.warehouse, sale_items)
        return sale


class SaleReturnItemSerializer(serializers.ModelSerializer):
    product_sku = serializers.CharField(source="product.sku", read_only=True)
    product_name = serializers.CharField(source="product.product_name", read_only=True)

    class Meta:
        model = SaleReturnItem
        fields = [
            "id", "product", "product_sku", "product_name",
            "quantity", "unit_price", "total",
        ]
        read_only_fields = ["total"]


class SaleReturnSerializer(serializers.ModelSerializer):
    sale_invoice = serializers.CharField(source="sale.invoice_number", read_only=True)
    customer_name = serializers.CharField(source="sale.customer.name", read_only=True)
    venture_name = serializers.CharField(source="venture.venture_code", read_only=True)
    items = SaleReturnItemSerializer(many=True, required=False)

    class Meta:
        model = SaleReturn
        fields = [
            "id", "venture", "venture_name", "sale", "sale_invoice",
            "customer_name", "return_number", "return_date", "status",
            "total_amount", "notes", "created_by", "items", "created_at",
        ]
        read_only_fields = ["return_number", "status", "total_amount", "created_by", "created_at"]

    def validate(self, attrs):
        venture = attrs.get("venture") or getattr(getattr(self, "instance", None), "venture", None)
        sale = attrs.get("sale", getattr(getattr(self, "instance", None), "sale", None))
        if sale is not None and sale.venture_id != venture.id:
            raise serializers.ValidationError(
                {"sale": ["Sale does not belong to the selected venture"]}
            )
        items = attrs.get("items", [])
        if sale is not None:
            sale_item_ids = set(sale.items.values_list("product_id", flat=True))
            for i, item in enumerate(items):
                if item["product"].venture_id != venture.id:
                    raise serializers.ValidationError(
                        {f"items.{i}.product": ["Product does not belong to the selected venture"]}
                    )
                if item["product"].id not in sale_item_ids:
                    raise serializers.ValidationError(
                        {f"items.{i}.product": ["Product was not part of this sale"]}
                    )
        return attrs

    def create(self, validated_data):
        items = validated_data.pop("items", [])
        request = self.context.get("request")
        user = getattr(request, "user", None)
        if user is not None and getattr(user, "is_authenticated", False):
            validated_data["created_by"] = user

        sale = validated_data["sale"]
        return_record = SaleReturn.objects.create(**validated_data)

        return_items = [
            SaleReturnItem(sale_return=return_record, **fields)
            for fields in items
        ]
        finalize_sale_return(request, return_record, return_items)
        return return_record