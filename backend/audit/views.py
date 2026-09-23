from django.db.models import Q
from rest_framework.pagination import PageNumberPagination
from rest_framework.permissions import IsAuthenticated
from rest_framework.views import APIView

from accounts.permissions import HasPermissionFromView
from core.responses import success

from .models import AuditLog
from .serializers import AuditLogSerializer


class AuditLogListView(APIView):
    """GET paginated, filterable audit trail."""

    permission_classes = [IsAuthenticated, HasPermissionFromView]
    required_permission = "audit.view"

    def get(self, request):
        queryset = AuditLog.objects.select_related("user").all()

        module = request.query_params.get("module", "").strip()
        if module:
            queryset = queryset.filter(module=module)

        action = request.query_params.get("action", "").strip().upper()
        if action:
            queryset = queryset.filter(action=action)

        object_id = request.query_params.get("object_id", "").strip()
        if object_id:
            queryset = queryset.filter(object_id=object_id)

        from_date = request.query_params.get("from", "").strip()
        if from_date:
            queryset = queryset.filter(created_at__date__gte=from_date)
        to_date = request.query_params.get("to", "").strip()
        if to_date:
            queryset = queryset.filter(created_at__date__lte=to_date)

        term = request.query_params.get("search", "").strip()
        if term:
            queryset = queryset.filter(
                Q(description__icontains=term)
                | Q(object_type__icontains=term)
                | Q(user__email__icontains=term)
            )

        paginator = PageNumberPagination()
        page = paginator.paginate_queryset(queryset, request)
        data = paginator.get_paginated_response(
            AuditLogSerializer(page, many=True).data
        ).data
        return success(data, message="Audit logs fetched")