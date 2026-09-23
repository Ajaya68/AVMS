"""Serializers for authentication, users and roles."""

from django.contrib.auth import get_user_model
from rest_framework import serializers
from rest_framework_simplejwt.serializers import TokenObtainPairSerializer

from .models import Role

User = get_user_model()


class RoleSerializer(serializers.ModelSerializer):
    permission_codes = serializers.ListField(
        child=serializers.CharField(), read_only=True
    )

    class Meta:
        model = Role
        fields = [
            "id",
            "code",
            "name",
            "description",
            "permission_codes",
            "is_active",
            "created_at",
            "updated_at",
        ]
        read_only_fields = ["id", "created_at", "updated_at"]


class UserSerializer(serializers.ModelSerializer):
    role_codes = serializers.ListField(
        child=serializers.CharField(), read_only=True
    )
    permissions = serializers.ListField(
        child=serializers.CharField(), read_only=True
    )

    class Meta:
        model = User
        fields = [
            "id",
            "email",
            "full_name",
            "phone",
            "is_active",
            "is_staff",
            "is_superuser",
            "role_codes",
            "roles",
            "permissions",
            "last_login",
            "created_at",
        ]
        read_only_fields = ["id", "permissions", "last_login", "created_at"]

    def to_representation(self, instance):
        data = super().to_representation(instance)
        data["roles"] = RoleSerializer(
            instance.roles.filter(is_active=True), many=True
        ).data
        return data


class UserCreateSerializer(serializers.ModelSerializer):
    email = serializers.EmailField()
    password = serializers.CharField(
        write_only=True, min_length=8, style={"input_type": "password"}
    )
    roles = serializers.PrimaryKeyRelatedField(
        queryset=Role.objects.filter(is_active=True),
        many=True,
        required=False,
        allow_empty=True,
    )

    class Meta:
        model = User
        fields = ["email", "password", "full_name", "phone", "roles"]

    def validate_email(self, value):
        if User.objects.filter(email__iexact=value).exists():
            raise serializers.ValidationError("A user with this email already exists.")
        return value.lower()

    def create(self, validated_data):
        roles = validated_data.pop("roles", [])
        user = User.objects.create_user(**validated_data)
        user.roles.set(roles)
        return user


class UserUpdateSerializer(serializers.ModelSerializer):
    roles = serializers.PrimaryKeyRelatedField(
        queryset=Role.objects.filter(is_active=True),
        many=True,
        required=False,
    )

    class Meta:
        model = User
        fields = ["full_name", "phone", "is_active", "roles"]


class LoginSerializer(TokenObtainPairSerializer):
    """Token pair using the email address as the username field."""

    username_field = User.USERNAME_FIELD

    def validate(self, attrs):
        data = super().validate(attrs)
        data["user"] = self.user
        return data


class RefreshSerializer(serializers.Serializer):
    refresh = serializers.CharField()