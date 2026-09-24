from django.db.models import Q
from rest_framework import status
from rest_framework.exceptions import ValidationError
from rest_framework.pagination import PageNumberPagination
from rest_framework.permissions import IsAuthenticated
from rest_framework.views import APIView

from accounts.permissions import HasPermission
from audit.models import AuditLog
from audit.services import log_audit
from core.responses import failure, success

from ventures.services import scope_queryset_by_venture

from .models import Purchase, PurchaseItem, PurchaseReturn
from .serializers import (
    PurchaseItemSerializer,
    PurchaseReturnSerializer,
    PurchaseSerializer,
)


class PurchaseListCreateView(APIView):
    pagination_class = PageNumberPagination

    def get_permissions(self):
        code = "purchases.manage" if self.request.method == "POST" else "purchases.view"
        return [IsAuthenticated(), HasPermission(code)]

    def get(self, request):
        queryset = Purchase.objects.select_related(
            "venture", "supplier", "warehouse"
        ).prefetch_related("items")
        queryset = scope_queryset_by_venture(queryset, request)

        term = request.query_params.get("search", "").strip()
        if term:
            queryset = queryset.filter(
                Q(invoice_number__icontains=term)
                | Q(supplier__name__icontains=term)
            )

        status_filter = request.query_params.get("status", "").strip().upper()
        if status_filter:
            queryset = queryset.filter(status=status_filter)

        paginator = self.pagination_class()
        page = paginator.paginate_queryset(queryset, request)
        data = paginator.get_paginated_response(
            PurchaseSerializer(page, many=True).data
        ).data
        return success(data, message="Purchases fetched")

    def post(self, request):
        serializer = PurchaseSerializer(data=request.data, context={"request": request})
        if not serializer.is_valid():
            return failure(
                message="Unable to create purchase",
                errors=serializer.errors,
                status=status.HTTP_400_BAD_REQUEST,
            )
        try:
            purchase = serializer.save()
        except ValidationError as exc:
            return failure(
                message="Unable to create purchase",
                errors=exc.detail,
                status=status.HTTP_400_BAD_REQUEST,
            )
        except ValueError as exc:
            return failure(
                message=str(exc),
                errors={"detail": [str(exc)]},
                status=status.HTTP_400_BAD_REQUEST,
            )

        log_audit(
            request,
            action=AuditLog.ACTION_CREATE,
            module="purchases",
            object_type="Purchase",
            object_id=purchase.id,
            description=f"Created purchase {purchase.invoice_number} - total {purchase.total_amount}",
        )
        return success(
            PurchaseSerializer(purchase).data,
            message=f"Purchase {purchase.invoice_number} recorded",
            status=status.HTTP_201_CREATED,
        )


class PurchaseDetailView(APIView):
    def get_permissions(self):
        if self.request.method == "GET":
            return [IsAuthenticated(), HasPermission("purchases.view")]
        return [IsAuthenticated(), HasPermission("purchases.manage")]

    def _get_purchase(self, pk):
        try:
            return scope_queryset_by_venture(
                Purchase.objects.select_related(
                    "venture", "supplier", "warehouse"
                ).prefetch_related("items", "items__product__unit"),
                self.request,
            ).get(pk=pk)
        except Purchase.DoesNotExist:
            return None

    def get(self, request, pk):
        purchase = self._get_purchase(pk)
        if purchase is None:
            return failure(message="Purchase not found", status=status.HTTP_404_NOT_FOUND)
        return success(PurchaseSerializer(purchase).data, message="Purchase fetched")

    def patch(self, request, pk):
        purchase = self._get_purchase(pk)
        if purchase is None:
            return failure(message="Purchase not found", status=status.HTTP_404_NOT_FOUND)

        serializer = PurchaseSerializer(purchase, data=request.data, partial=True)
        if not serializer.is_valid():
            return failure(
                message="Unable to update purchase",
                errors=serializer.errors,
                status=status.HTTP_400_BAD_REQUEST,
            )
        purchase = serializer.save()
        purchase.recompute()
        purchase.save(update_fields=["subtotal", "total_amount", "due_amount"])

        log_audit(
            request,
            action=AuditLog.ACTION_UPDATE,
            module="purchases",
            object_type="Purchase",
            object_id=purchase.id,
            description=f"Updated purchase {purchase.invoice_number}",
        )
        return success(PurchaseSerializer(purchase).data, message="Purchase updated")

    def delete(self, request, pk):
        purchase = self._get_purchase(pk)
        if purchase is None:
            return failure(message="Purchase not found", status=status.HTTP_404_NOT_FOUND)

        if purchase.warehouse_id is not None:
            return failure(
                message="Cannot delete a purchase whose stock was received; reverse it with a purchase return instead",
                status=status.HTTP_400_BAD_REQUEST,
            )

        number = purchase.invoice_number
        purchase.delete()
        log_audit(
            request,
            action=AuditLog.ACTION_DELETE,
            module="purchases",
            object_type="Purchase",
            object_id=pk,
            description=f"Deleted purchase {number}",
        )
        return success(None, message="Purchase deleted")


class PurchaseItemsView(APIView):
    def get_permissions(self):
        return [IsAuthenticated(), HasPermission("purchases.view")]

    def get(self, request, pk):
        try:
            scope_queryset_by_venture(Purchase.objects.all(), request).get(pk=pk)
        except Purchase.DoesNotExist:
            return failure(message="Purchase not found", status=status.HTTP_404_NOT_FOUND)
        items = PurchaseItem.objects.filter(purchase_id=pk).select_related("product__unit")
        return success(
            PurchaseItemSerializer(items, many=True).data, message="Purchase items fetched"
        )


class PurchaseReturnListCreateView(APIView):
    pagination_class = PageNumberPagination

    def get_permissions(self):
        code = (
            "purchase_returns.manage"
            if self.request.method == "POST"
            else "purchase_returns.view"
        )
        return [IsAuthenticated(), HasPermission(code)]

    def get(self, request):
        queryset = PurchaseReturn.objects.select_related(
            "venture", "purchase", "purchase__supplier"
        ).prefetch_related("items")
        queryset = scope_queryset_by_venture(queryset, request)

        term = request.query_params.get("search", "").strip()
        if term:
            queryset = queryset.filter(
                Q(return_number__icontains=term)
                | Q(purchase__invoice_number__icontains=term)
            )

        paginator = self.pagination_class()
        page = paginator.paginate_queryset(queryset, request)
        data = paginator.get_paginated_response(
            PurchaseReturnSerializer(page, many=True).data
        ).data
        return success(data, message="Purchase returns fetched")

    def post(self, request):
        serializer = PurchaseReturnSerializer(
            data=request.data, context={"request": request}
        )
        if not serializer.is_valid():
            return failure(
                message="Unable to record purchase return",
                errors=serializer.errors,
                status=status.HTTP_400_BAD_REQUEST,
            )
        try:
            return_record = serializer.save()
        except ValidationError as exc:
            return failure(
                message="Unable to record purchase return",
                errors=exc.detail,
                status=status.HTTP_400_BAD_REQUEST,
            )
        except ValueError as exc:
            return failure(
                message=str(exc),
                errors={"detail": [str(exc)]},
                status=status.HTTP_400_BAD_REQUEST,
            )

        log_audit(
            request,
            action=AuditLog.ACTION_CREATE,
            module="purchase_returns",
            object_type="PurchaseReturn",
            object_id=return_record.id,
            description=f"Purchase return {return_record.return_number} "
            f"on {return_record.purchase.invoice_number}",
        )
        return success(
            PurchaseReturnSerializer(return_record).data,
            message="Purchase return recorded",
            status=status.HTTP_201_CREATED,
        )


class PurchaseReturnDetailView(APIView):
    def get_permissions(self):
        return [IsAuthenticated(), HasPermission("purchase_returns.view")]

    def get(self, request, pk):
        try:
            return_record = scope_queryset_by_venture(
                PurchaseReturn.objects.select_related(
                    "venture", "purchase", "purchase__supplier"
                ).prefetch_related("items"),
                request,
            ).get(pk=pk)
        except PurchaseReturn.DoesNotExist:
            return failure(
                message="Purchase return not found", status=status.HTTP_404_NOT_FOUND
            )
        return success(
            PurchaseReturnSerializer(return_record).data,
            message="Purchase return fetched",
        )