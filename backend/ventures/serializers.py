"""Serializers for the Venture model."""

from rest_framework import serializers

from .models import Venture


class VentureSerializer(serializers.ModelSerializer):
    class Meta:
        model = Venture
        fields = [
            "id",
            "venture_code",
            "venture_name",
            "description",
            "business_type",
            "phone",
            "email",
            "address",
            "city",
            "state",
            "pincode",
            "status",
            "created_at",
            "updated_at",
        ]
        read_only_fields = ["id", "venture_code", "created_at", "updated_at"]