from rest_framework import status
from rest_framework.pagination import PageNumberPagination
from rest_framework.permissions import IsAuthenticated
from rest_framework.views import APIView

from accounts.permissions import HasPermissionFromView
from audit.models import AuditLog
from audit.serializers import AuditLogSerializer
from core.responses import failure, success

from .models import Notification
from .serializers import NotificationSerializer


class NotificationListView(APIView):
    """GET own notifications (paginated, filters), POST create for a user."""

    permission_classes = [IsAuthenticated, HasPermissionFromView]
    required_permission = "notifications.view"

    def get(self, request):
        queryset = Notification.objects.filter(user=request.user)
        unread_first = request.query_params.get("unread_first", "").lower() == "true"
        if unread_first:
            queryset = queryset.order_by("-is_read", "-created_at", "-id")

        paginator = PageNumberPagination()
        page = paginator.paginate_queryset(queryset, request)
        data = paginator.get_paginated_response(
            NotificationSerializer(page, many=True).data
        ).data
        return success(
            data,
            message=f"Notifications fetched ({unread_first and 'unread first' or 'newest first'})",
        )


class NotificationUnreadCountView(APIView):
    permission_classes = [IsAuthenticated, HasPermissionFromView]
    required_permission = "notifications.view"

    def get(self, request):
        count = Notification.objects.filter(user=request.user, is_read=False).count()
        return success({"count": count}, message="Unread notification count")


class NotificationReadView(APIView):
    permission_classes = [IsAuthenticated, HasPermissionFromView]
    required_permission = "notifications.view"

    def _get(self, request, pk):
        try:
            return Notification.objects.get(pk=pk, user=request.user)
        except Notification.DoesNotExist:
            return None

    def post(self, request, pk):
        notification = self._get(request, pk)
        if notification is None:
            return failure(
                message="Notification not found", status=status.HTTP_404_NOT_FOUND
            )
        notification.is_read = True
        notification.save(update_fields=["is_read"])
        return success(
            NotificationSerializer(notification).data, message="Notification marked read"
        )


class NotificationReadAllView(APIView):
    permission_classes = [IsAuthenticated, HasPermissionFromView]
    required_permission = "notifications.view"

    def post(self, request):
        updated = Notification.objects.filter(
            user=request.user, is_read=False
        ).update(is_read=True)
        return success({"count": updated}, message="All notifications marked read")