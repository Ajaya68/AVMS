"""Reusable CRUD views for AVMS feature modules.

Modules follow the same contract: ``{module}.view`` gates reads,
``{module}.manage`` gates writes, results are optionally scoped to the venture
in the ``X-Venture-Id`` header, keyword search + status filter supported, and
every create/update/delete writes an :class:`~audit.models.AuditLog` entry.
"""

from django.db.models import Q
from rest_framework import status
from rest_framework.pagination import PageNumberPagination
from rest_framework.permissions import IsAuthenticated
from rest_framework.views import APIView

from accounts.permissions import HasPermission
from audit.models import AuditLog
from audit.services import log_audit
from core.responses import failure, success

from ventures.services import scope_queryset_by_venture


class MasterListCreateView(APIView):
    """GET paginated, searchable list; POST create."""

    pagination_class = PageNumberPagination
    model = None
    serializer_class = None
    permission_module = None  # e.g. "customers"
    search_fields = []  # eg ["venture_name", "city"]
    scope_by_venture = True
    audit_object_type = ""

    def _queryset(self, request):
        queryset = self.model.objects.all()
        if self.scope_by_venture and getattr(self.model, "venture", None):
            queryset = scope_queryset_by_venture(queryset, request)
        return queryset

    def get_permissions(self):
        code = (
            f"{self.permission_module}.manage"
            if self.request.method == "POST"
            else f"{self.permission_module}.view"
        )
        return [IsAuthenticated(), HasPermission(code)]

    def get(self, request):
        queryset = self._queryset(request)

        term = request.query_params.get("search", "").strip()
        if term and self.search_fields:
            q = Q()
            for field in self.search_fields:
                q |= Q(**{f"{field}__icontains": term})
            queryset = queryset.filter(q)

        status_filter = request.query_params.get("status", "").strip().upper()
        if status_filter and hasattr(self.model, "status"):
            queryset = queryset.filter(status=status_filter)

        paginator = self.pagination_class()
        page = paginator.paginate_queryset(queryset, request)
        data = paginator.get_paginated_response(
            self.serializer_class(page, many=True).data
        ).data
        return success(data, message=f"{self.model._meta.verbose_name_plural} fetched")

    def post(self, request):
        serializer = self.serializer_class(data=request.data)
        if not serializer.is_valid():
            return failure(
                message=f"Unable to create {self.model._meta.verbose_name}",
                errors=serializer.errors,
                status=status.HTTP_400_BAD_REQUEST,
            )
        instance = serializer.save()

        log_audit(
            request,
            action=AuditLog.ACTION_CREATE,
            module=self.permission_module,
            object_type=self.audit_object_type,
            object_id=instance.id,
            description=f"Created {self.audit_object_type} {getattr(instance, 'name', instance.id)}",
        )
        return success(
            self.serializer_class(instance).data,
            message=f"{self.audit_object_type} created",
            status=status.HTTP_201_CREATED,
        )


class MasterDetailView(APIView):
    """GET / PATCH / DELETE a single record."""

    model = None
    serializer_class = None
    permission_module = None
    audit_object_type = ""

    def get_permissions(self):
        if self.request.method == "GET":
            return [IsAuthenticated(), HasPermission(f"{self.permission_module}.view")]
        return [IsAuthenticated(), HasPermission(f"{self.permission_module}.manage")]

    def _get_object(self, pk):
        queryset = self.model.objects.all()
        if getattr(self.model, "venture", None):
            queryset = scope_queryset_by_venture(queryset, self.request)
        try:
            return queryset.get(pk=pk)
        except self.model.DoesNotExist:
            return None

    def get(self, request, pk):
        instance = self._get_object(pk)
        if instance is None:
            return failure(message=f"{self.audit_object_type} not found", status=status.HTTP_404_NOT_FOUND)
        return success(self.serializer_class(instance).data, message=f"{self.audit_object_type} fetched")

    def patch(self, request, pk):
        instance = self._get_object(pk)
        if instance is None:
            return failure(message=f"{self.audit_object_type} not found", status=status.HTTP_404_NOT_FOUND)

        serializer = self.serializer_class(instance, data=request.data, partial=True)
        if not serializer.is_valid():
            return failure(
                message=f"Unable to update {self.audit_object_type}",
                errors=serializer.errors,
                status=status.HTTP_400_BAD_REQUEST,
            )
        instance = serializer.save()

        log_audit(
            request,
            action=AuditLog.ACTION_UPDATE,
            module=self.permission_module,
            object_type=self.audit_object_type,
            object_id=instance.id,
            description=f"Updated {self.audit_object_type} {getattr(instance, 'name', instance.id)}",
        )
        return success(self.serializer_class(instance).data, message=f"{self.audit_object_type} updated")

    def delete(self, request, pk):
        instance = self._get_object(pk)
        if instance is None:
            return failure(message=f"{self.audit_object_type} not found", status=status.HTTP_404_NOT_FOUND)

        label = getattr(instance, "name", instance.id)
        instance.delete()

        log_audit(
            request,
            action=AuditLog.ACTION_DELETE,
            module=self.permission_module,
            object_type=self.audit_object_type,
            object_id=pk,
            description=f"Deleted {self.audit_object_type} {label}",
        )
        return success(None, message=f"{self.audit_object_type} deleted")