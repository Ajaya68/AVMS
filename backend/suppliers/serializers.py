from rest_framework import serializers

from .models import Supplier


class SupplierSerializer(serializers.ModelSerializer):
    venture_name = serializers.CharField(source="venture.venture_code", read_only=True)

    class Meta:
        model = Supplier
        fields = [
            "id",
            "venture",
            "venture_name",
            "supplier_code",
            "name",
            "contact_person",
            "phone",
            "email",
            "address",
            "city",
            "state",
            "pincode",
            "gst_number",
            "payment_terms",
            "status",
            "created_at",
            "updated_at",
        ]
        read_only_fields = ["supplier_code", "created_at", "updated_at"]