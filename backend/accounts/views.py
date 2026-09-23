"""Authentication, user and role views.

All business-rule reasoning lives either in serializers or the small service
layer here. Responses use the shared envelope from ``core.responses``.
"""

from django.contrib.auth import get_user_model
from django.db.models import Q
from rest_framework import status
from rest_framework.pagination import PageNumberPagination
from rest_framework.permissions import IsAuthenticated
from rest_framework.views import APIView
from rest_framework_simplejwt.exceptions import InvalidToken, TokenError
from rest_framework_simplejwt.tokens import RefreshToken
from rest_framework_simplejwt.views import TokenRefreshView

from audit.models import AuditLog
from audit.services import log_audit
from core.responses import failure, success

from .models import Role
from .permissions import IsAdmin
from .serializers import (
    LoginSerializer,
    RoleSerializer,
    UserCreateSerializer,
    UserSerializer,
    UserUpdateSerializer,
)

User = get_user_model()


class LoginView(APIView):
    """POST email + password -> { access, refresh, user }."""

    authentication_classes = []
    permission_classes = []

    def get_authenticate_header(self, request):
        # Provide WWW-Authenticate so rejected credentials stay a clean 401
        # instead of DRF's default 403 coercion when no authenticator is set.
        return 'Bearer realm="api"'

    def post(self, request):
        serializer = LoginSerializer(data=request.data)
        try:
            serializer.is_valid(raise_exception=True)
        except TokenError as exc:
            raise InvalidToken(exc.args[0]) from exc

        token_data = serializer.validated_data
        user = token_data.pop("user")

        log_audit(
            request,
            action=AuditLog.ACTION_LOGIN,
            module="auth",
            description=f"User logged in: {user.email}",
            user=user,
        )

        payload = {
            "access": token_data["access"],
            "refresh": token_data["refresh"],
            "user": UserSerializer(user).data,
        }
        return success(payload, message="Login successful")


class RefreshView(TokenRefreshView):
    """Standard token refresh. Returns a rotated access + refresh pair."""


class LogoutView(APIView):
    """POST refresh token to invalidate (blacklist) the session."""

    def post(self, request):
        refresh = request.data.get("refresh")
        if not refresh:
            return failure(
                message="Refresh token is required", status=status.HTTP_400_BAD_REQUEST
            )
        try:
            token = RefreshToken(refresh)
            token.blacklist()
        except TokenError as exc:
            return failure(
                message="Invalid or expired refresh token",
                errors={"refresh": [str(exc)]},
                status=status.HTTP_400_BAD_REQUEST,
            )

        log_audit(
            request,
            action=AuditLog.ACTION_LOGOUT,
            module="auth",
            description="User logged out",
        )
        return success(None, message="Logged out successfully")


class MeView(APIView):
    """GET current user profile including roles and granted capabilities."""

    permission_classes = [IsAuthenticated]

    def get(self, request):
        return success(UserSerializer(request.user).data, message="Profile fetched")


class UserListCreateView(APIView):
    """GET paginated users, POST create a user (ADMIN role required)."""

    permission_classes = [IsAuthenticated, IsAdmin]
    pagination_class = PageNumberPagination

    def get(self, request):
        queryset = User.objects.order_by("-created_at").prefetch_related("roles")
        search = request.query_params.get("search", "").strip()
        if search:
            queryset = queryset.filter(
                Q(email__icontains=search) | Q(full_name__icontains=search)
            )

        paginator = self.pagination_class()
        page = paginator.paginate_queryset(queryset, request)
        data = paginator.get_paginated_response(
            UserSerializer(page, many=True).data
        ).data
        return success(data, message="Users fetched")

    def post(self, request):
        serializer = UserCreateSerializer(data=request.data)
        if not serializer.is_valid():
            return failure(
                message="Unable to create user",
                errors=serializer.errors,
                status=status.HTTP_400_BAD_REQUEST,
            )
        user = serializer.save()

        log_audit(
            request,
            action=AuditLog.ACTION_USER_CREATED,
            module="auth",
            object_type="User",
            object_id=user.id,
            description=f"Created user {user.email}",
        )
        return success(
            UserSerializer(user).data,
            message="User created",
            status=status.HTTP_201_CREATED,
        )


class UserDetailView(APIView):
    """GET / PATCH a single user (ADMIN role required)."""

    permission_classes = [IsAuthenticated, IsAdmin]

    def _get_user(self, pk):
        try:
            return User.objects.get(pk=pk)
        except User.DoesNotExist:
            return None

    def get(self, request, pk):
        user = self._get_user(pk)
        if user is None:
            return failure(message="User not found", status=status.HTTP_404_NOT_FOUND)
        return success(UserSerializer(user).data, message="User fetched")

    def patch(self, request, pk):
        user = self._get_user(pk)
        if user is None:
            return failure(message="User not found", status=status.HTTP_404_NOT_FOUND)

        serializer = UserUpdateSerializer(user, data=request.data, partial=True)
        if not serializer.is_valid():
            return failure(
                message="Unable to update user",
                errors=serializer.errors,
                status=status.HTTP_400_BAD_REQUEST,
            )
        serializer.save()

        log_audit(
            request,
            action=AuditLog.ACTION_UPDATE,
            module="auth",
            object_type="User",
            object_id=user.id,
            description=f"Updated user {user.email}",
        )
        return success(UserSerializer(user).data, message="User updated")


class RoleListView(APIView):
    """GET the list of active roles with their permission codes."""

    permission_classes = [IsAuthenticated, IsAdmin]

    def get(self, request):
        roles = Role.objects.filter(is_active=True).prefetch_related("permissions")
        return success(
            RoleSerializer(roles, many=True).data, message="Roles fetched"
        )