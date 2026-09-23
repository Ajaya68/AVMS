from rest_framework import serializers

from .models import AuditLog


class AuditLogSerializer(serializers.ModelSerializer):
    user_email = serializers.CharField(source="user.email", read_only=True)

    class Meta:
        model = AuditLog
        fields = [
            "id", "user", "user_email", "action", "module",
            "object_type", "object_id", "ip_address", "description", "created_at",
        ]