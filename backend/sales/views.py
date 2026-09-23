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

from .models import Sale, SaleItem, SaleReturn
from .serializers import SaleItemSerializer, SaleReturnSerializer, SaleSerializer


class SaleListCreateView(APIView):
    pagination_class = PageNumberPagination

    def get_permissions(self):
        code = "sales.manage" if self.request.method == "POST" else "sales.view"
        return [IsAuthenticated(), HasPermission(code)]

    def get(self, request):
        queryset = Sale.objects.select_related(
            "venture", "customer", "warehouse"
        ).prefetch_related("items")
        queryset = scope_queryset_by_venture(queryset, request)

        term = request.query_params.get("search", "").strip()
        if term:
            queryset = queryset.filter(
                Q(invoice_number__icontains=term)
                | Q(customer__name__icontains=term)
            )

        status_filter = request.query_params.get("status", "").strip().upper()
        if status_filter:
            queryset = queryset.filter(status=status_filter)

        paginator = self.pagination_class()
        page = paginator.paginate_queryset(queryset, request)
        data = paginator.get_paginated_response(
            SaleSerializer(page, many=True).data
        ).data
        return success(data, message="Sales fetched")

    def post(self, request):
        serializer = SaleSerializer(data=request.data, context={"request": request})
        if not serializer.is_valid():
            return failure(
                message="Unable to create sale",
                errors=serializer.errors,
                status=status.HTTP_400_BAD_REQUEST,
            )
        try:
            sale = serializer.save()
        except ValidationError as exc:
            return failure(
                message="Unable to create sale",
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
            module="sales",
            object_type="Sale",
            object_id=sale.id,
            description=f"Created sale {sale.invoice_number} - total {sale.total_amount}",
        )
        return success(
            SaleSerializer(sale).data,
            message=f"Sale {sale.invoice_number} recorded",
            status=status.HTTP_201_CREATED,
        )


class SaleDetailView(APIView):
    def get_permissions(self):
        if self.request.method == "GET":
            return [IsAuthenticated(), HasPermission("sales.view")]
        return [IsAuthenticated(), HasPermission("sales.manage")]

    def _get_sale(self, pk):
        try:
            return Sale.objects.select_related(
                "venture", "customer", "warehouse"
            ).prefetch_related("items", "items__product__unit").get(pk=pk)
        except Sale.DoesNotExist:
            return None

    def get(self, request, pk):
        sale = self._get_sale(pk)
        if sale is None:
            return failure(message="Sale not found", status=status.HTTP_404_NOT_FOUND)
        return success(SaleSerializer(sale).data, message="Sale fetched")

    def patch(self, request, pk):
        sale = self._get_sale(pk)
        if sale is None:
            return failure(message="Sale not found", status=status.HTTP_404_NOT_FOUND)

        serializer = SaleSerializer(sale, data=request.data, partial=True)
        if not serializer.is_valid():
            return failure(
                message="Unable to update sale",
                errors=serializer.errors,
                status=status.HTTP_400_BAD_REQUEST,
            )
        sale = serializer.save()
        sale.recompute()
        sale.save(update_fields=["subtotal", "total_amount", "due_amount"])

        log_audit(
            request,
            action=AuditLog.ACTION_UPDATE,
            module="sales",
            object_type="Sale",
            object_id=sale.id,
            description=f"Updated sale {sale.invoice_number}",
        )
        return success(SaleSerializer(sale).data, message="Sale updated")

    def delete(self, request, pk):
        sale = self._get_sale(pk)
        if sale is None:
            return failure(message="Sale not found", status=status.HTTP_404_NOT_FOUND)

        if sale.warehouse_id is not None:
            return failure(
                message="Cannot delete a sale whose stock was dispatched; reverse it with a sale return instead",
                status=status.HTTP_400_BAD_REQUEST,
            )

        number = sale.invoice_number
        sale.delete()
        log_audit(
            request,
            action=AuditLog.ACTION_DELETE,
            module="sales",
            object_type="Sale",
            object_id=pk,
            description=f"Deleted sale {number}",
        )
        return success(None, message="Sale deleted")


class SaleItemsView(APIView):
    def get_permissions(self):
        return [IsAuthenticated(), HasPermission("sales.view")]

    def get(self, request, pk):
        try:
            Sale.objects.get(pk=pk)
        except Sale.DoesNotExist:
            return failure(message="Sale not found", status=status.HTTP_404_NOT_FOUND)
        items = SaleItem.objects.filter(sale_id=pk).select_related("product__unit")
        return success(
            SaleItemSerializer(items, many=True).data, message="Sale items fetched"
        )


class SaleReturnListCreateView(APIView):
    pagination_class = PageNumberPagination

    def get_permissions(self):
        code = (
            "sales_returns.manage"
            if self.request.method == "POST"
            else "sales_returns.view"
        )
        return [IsAuthenticated(), HasPermission(code)]

    def get(self, request):
        queryset = SaleReturn.objects.select_related(
            "venture", "sale", "sale__customer"
        ).prefetch_related("items")
        queryset = scope_queryset_by_venture(queryset, request)

        term = request.query_params.get("search", "").strip()
        if term:
            queryset = queryset.filter(
                Q(return_number__icontains=term)
                | Q(sale__invoice_number__icontains=term)
            )

        paginator = self.pagination_class()
        page = paginator.paginate_queryset(queryset, request)
        data = paginator.get_paginated_response(
            SaleReturnSerializer(page, many=True).data
        ).data
        return success(data, message="Sale returns fetched")

    def post(self, request):
        serializer = SaleReturnSerializer(
            data=request.data, context={"request": request}
        )
        if not serializer.is_valid():
            return failure(
                message="Unable to record sale return",
                errors=serializer.errors,
                status=status.HTTP_400_BAD_REQUEST,
            )
        try:
            return_record = serializer.save()
        except ValidationError as exc:
            return failure(
                message="Unable to record sale return",
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
            module="sales_returns",
            object_type="SaleReturn",
            object_id=return_record.id,
            description=f"Sale return {return_record.return_number} "
            f"on {return_record.sale.invoice_number}",
        )
        return success(
            SaleReturnSerializer(return_record).data,
            message="Sale return recorded",
            status=status.HTTP_201_CREATED,
        )


class SaleReturnDetailView(APIView):
    def get_permissions(self):
        return [IsAuthenticated(), HasPermission("sales_returns.view")]

    def get(self, request, pk):
        try:
            return_record = SaleReturn.objects.select_related(
                "venture", "sale", "sale__customer"
            ).prefetch_related("items").get(pk=pk)
        except SaleReturn.DoesNotExist:
            return failure(
                message="Sale return not found", status=status.HTTP_404_NOT_FOUND
            )
        return success(
            SaleReturnSerializer(return_record).data,
            message="Sale return fetched",
        )