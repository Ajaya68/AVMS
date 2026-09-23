from rest_framework import serializers

from .models import Notification


class NotificationSerializer(serializers.ModelSerializer):
    user_email = serializers.CharField(source="user.email", read_only=True)

    class Meta:
        model = Notification
        fields = [
            "id", "user", "user_email", "type", "message",
            "link", "is_read", "created_at",
        ]
        read_only_fields = ["user", "is_read", "created_at"]