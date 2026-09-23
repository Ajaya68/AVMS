from rest_framework import serializers

from .models import Customer


class CustomerSerializer(serializers.ModelSerializer):
    venture_name = serializers.CharField(source="venture.venture_code", read_only=True)

    class Meta:
        model = Customer
        fields = [
            "id",
            "venture",
            "venture_name",
            "customer_code",
            "name",
            "phone",
            "email",
            "address",
            "city",
            "state",
            "pincode",
            "gst_number",
            "credit_limit",
            "status",
            "created_at",
            "updated_at",
        ]
        read_only_fields = ["customer_code", "created_at", "updated_at"]