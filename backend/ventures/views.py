"""Venture CRUD views.

GET (list/retrieve) requires ``ventures.view``; write operations require
``ventures.manage``. Permissions are resolved per-request method so a single
view pair serves both scopes.
"""

from rest_framework import status
from rest_framework.pagination import PageNumberPagination
from rest_framework.permissions import IsAuthenticated
from rest_framework.views import APIView

from accounts.permissions import HasPermission
from audit.models import AuditLog
from audit.services import log_audit
from core.responses import failure, success

from .models import Venture
from .serializers import VentureSerializer
from .services import search_ventures


class VentureListCreateView(APIView):
    """GET paginated ventures, POST create a venture."""

    pagination_class = PageNumberPagination

    def get_permissions(self):
        code = "ventures.manage" if self.request.method == "POST" else "ventures.view"
        return [IsAuthenticated(), HasPermission(code)]

    def get(self, request):
        queryset = Venture.objects.all()
        term = request.query_params.get("search", "").strip()
        if term:
            queryset = search_ventures(queryset, term)

        status_filter = request.query_params.get("status", "").strip().upper()
        if status_filter:
            queryset = queryset.filter(status=status_filter)

        paginator = self.pagination_class()
        page = paginator.paginate_queryset(queryset, request)
        data = paginator.get_paginated_response(
            VentureSerializer(page, many=True).data
        ).data
        return success(data, message="Ventures fetched")

    def post(self, request):
        serializer = VentureSerializer(data=request.data)
        if not serializer.is_valid():
            return failure(
                message="Unable to create venture",
                errors=serializer.errors,
                status=status.HTTP_400_BAD_REQUEST,
            )
        venture = serializer.save()

        log_audit(
            request,
            action=AuditLog.ACTION_CREATE,
            module="ventures",
            object_type="Venture",
            object_id=venture.id,
            description=f"Created venture {venture.venture_code} - {venture.venture_name}",
        )
        return success(
            VentureSerializer(venture).data,
            message="Venture created",
            status=status.HTTP_201_CREATED,
        )


class VentureDetailView(APIView):
    """GET / PATCH / DELETE a single venture."""

    def get_permissions(self):
        if self.request.method == "GET":
            return [IsAuthenticated(), HasPermission("ventures.view")]
        return [IsAuthenticated(), HasPermission("ventures.manage")]

    def _get_venture(self, pk):
        try:
            return Venture.objects.get(pk=pk)
        except Venture.DoesNotExist:
            return None

    def get(self, request, pk):
        venture = self._get_venture(pk)
        if venture is None:
            return failure(message="Venture not found", status=status.HTTP_404_NOT_FOUND)
        return success(VentureSerializer(venture).data, message="Venture fetched")

    def patch(self, request, pk):
        venture = self._get_venture(pk)
        if venture is None:
            return failure(message="Venture not found", status=status.HTTP_404_NOT_FOUND)

        serializer = VentureSerializer(venture, data=request.data, partial=True)
        if not serializer.is_valid():
            return failure(
                message="Unable to update venture",
                errors=serializer.errors,
                status=status.HTTP_400_BAD_REQUEST,
            )
        venture = serializer.save()

        log_audit(
            request,
            action=AuditLog.ACTION_UPDATE,
            module="ventures",
            object_type="Venture",
            object_id=venture.id,
            description=f"Updated venture {venture.venture_code}",
        )
        return success(
            VentureSerializer(venture).data, message="Venture updated"
        )

    def delete(self, request, pk):
        venture = self._get_venture(pk)
        if venture is None:
            return failure(message="Venture not found", status=status.HTTP_404_NOT_FOUND)

        code_name = f"{venture.venture_code} - {venture.venture_name}"
        venture.delete()

        log_audit(
            request,
            action=AuditLog.ACTION_DELETE,
            module="ventures",
            object_type="Venture",
            object_id=pk,
            description=f"Deleted venture {code_name}",
        )
        return success(None, message="Venture deleted")