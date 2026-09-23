from django.db.models import F, Q
from rest_framework import status
from rest_framework.exceptions import ValidationError
from rest_framework.pagination import PageNumberPagination
from rest_framework.permissions import IsAuthenticated
from rest_framework.views import APIView

from accounts.permissions import HasPermission
from audit.models import AuditLog
from audit.services import log_audit
from core.crud_views import MasterDetailView, MasterListCreateView
from core.responses import failure, success

from ventures.services import scope_queryset_by_venture

from .models import Inventory, StockMovement, Warehouse
from .serializers import InventorySerializer, StockMovementSerializer, WarehouseSerializer


class WarehouseListCreateView(MasterListCreateView):
    model = Warehouse
    serializer_class = WarehouseSerializer
    permission_module = "warehouses"
    search_fields = ["warehouse_name", "warehouse_code", "city", "manager"]
    audit_object_type = "Warehouse"


class WarehouseDetailView(MasterDetailView):
    model = Warehouse
    serializer_class = WarehouseSerializer
    permission_module = "warehouses"
    audit_object_type = "Warehouse"


class InventoryListView(APIView):
    """GET stock on hand, optionally filtered by warehouse, product or low stock."""

    pagination_class = PageNumberPagination

    def get_permissions(self):
        return [IsAuthenticated(), HasPermission("inventory.view")]

    def get(self, request):
        queryset = Inventory.objects.select_related(
            "warehouse", "product", "product__unit", "venture"
        )
        queryset = scope_queryset_by_venture(queryset, request)

        warehouse = request.query_params.get("warehouse", "").strip()
        if warehouse:
            queryset = queryset.filter(warehouse_id=warehouse)

        product = request.query_params.get("product", "").strip()
        if product:
            queryset = queryset.filter(product_id=product)

        low = request.query_params.get("low", "").lower()
        if low in ("true", "1"):
            queryset = queryset.filter(
                Q(quantity__lte=F("reserved_quantity") + F("reorder_level"))
            )

        paginator = self.pagination_class()
        page = paginator.paginate_queryset(queryset, request)
        data = paginator.get_paginated_response(
            InventorySerializer(page, many=True).data
        ).data
        return success(data, message="Inventory fetched")


class StockMovementListCreateView(APIView):
    pagination_class = PageNumberPagination

    def get_permissions(self):
        code = (
            "stock_movements.manage"
            if self.request.method == "POST"
            else "stock_movements.view"
        )
        return [IsAuthenticated(), HasPermission(code)]

    def get(self, request):
        queryset = StockMovement.objects.select_related(
            "warehouse", "product", "venture", "destination_warehouse"
        )
        queryset = scope_queryset_by_venture(queryset, request)

        product = request.query_params.get("product", "").strip()
        if product:
            queryset = queryset.filter(product_id=product)
        warehouse = request.query_params.get("warehouse", "").strip()
        if warehouse:
            queryset = queryset.filter(warehouse_id=warehouse)
        movement_type = request.query_params.get("movement_type", "").strip()
        if movement_type:
            queryset = queryset.filter(movement_type=movement_type)

        paginator = self.pagination_class()
        page = paginator.paginate_queryset(queryset, request)
        data = paginator.get_paginated_response(
            StockMovementSerializer(page, many=True, context={"request": request}).data
        ).data
        return success(data, message="Stock movements fetched")

    def post(self, request):
        serializer = StockMovementSerializer(
            data=request.data, context={"request": request}
        )
        if not serializer.is_valid():
            return failure(
                message="Unable to record stock movement",
                errors=serializer.errors,
                status=status.HTTP_400_BAD_REQUEST,
            )
        try:
            movement = serializer.save()
        except ValidationError as exc:
            return failure(
                message="Unable to record stock movement",
                errors=exc.detail,
                status=status.HTTP_400_BAD_REQUEST,
            )

        paired = serializer.context.get("paired")
        log_audit(
            request,
            action=AuditLog.ACTION_CREATE,
            module="stock_movements",
            object_type="StockMovement",
            object_id=movement.id,
            description=f"{movement.movement_type} {movement.quantity:g} x "
            f"{movement.product.sku} @ {movement.warehouse.warehouse_code}",
        )
        return success(
            StockMovementSerializer(movement, context={"request": request}).data,
            message="Stock movement recorded",
            status=status.HTTP_201_CREATED,
        )


class StockMovementDetailView(APIView):
    def get_permissions(self):
        return [IsAuthenticated(), HasPermission("stock_movements.view")]

    def get(self, request, pk):
        try:
            movement = StockMovement.objects.select_related(
                "warehouse", "product", "venture"
            ).get(pk=pk)
        except StockMovement.DoesNotExist:
            return failure(
                message="Stock movement not found", status=status.HTTP_404_NOT_FOUND
            )
        return success(
            StockMovementSerializer(movement, context={"request": request}).data,
            message="Stock movement fetched",
        )