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

from .models import Payment
from .serializers import PaymentSerializer
from .services import reverse_payment


class PaymentListCreateView(APIView):
    pagination_class = PageNumberPagination

    def get_permissions(self):
        code = "payments.manage" if self.request.method == "POST" else "payments.view"
        return [IsAuthenticated(), HasPermission(code)]

    def get(self, request):
        queryset = Payment.objects.select_related("venture").filter()
        queryset = scope_queryset_by_venture(queryset, request)

        payment_type = request.query_params.get("payment_type", "").strip().upper()
        if payment_type:
            queryset = queryset.filter(payment_type=payment_type)

        reference_type = request.query_params.get("reference_type", "").strip().upper()
        if reference_type:
            queryset = queryset.filter(reference_type=reference_type)

        term = request.query_params.get("search", "").strip()
        if term:
            queryset = queryset.filter(
                Q(notes__icontains=term)
                | Q(transaction_reference__icontains=term)
            )

        paginator = self.pagination_class()
        page = paginator.paginate_queryset(queryset, request)
        data = paginator.get_paginated_response(
            PaymentSerializer(page, many=True).data
        ).data
        return success(data, message="Payments fetched")

    def post(self, request):
        serializer = PaymentSerializer(data=request.data, context={"request": request})
        if not serializer.is_valid():
            return failure(
                message="Unable to record payment",
                errors=serializer.errors,
                status=status.HTTP_400_BAD_REQUEST,
            )
        try:
            payment = serializer.save()
        except ValidationError as exc:
            return failure(
                message="Unable to record payment",
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
            module="payments",
            object_type="Payment",
            object_id=payment.id,
            description=f"Payment {payment.payment_type} {payment.amount} "
            f"on {payment.reference_type} {payment.reference_id}",
        )
        return success(
            PaymentSerializer(payment).data,
            message="Payment recorded",
            status=status.HTTP_201_CREATED,
        )


class PaymentDetailView(APIView):
    def get_permissions(self):
        if self.request.method == "GET":
            return [IsAuthenticated(), HasPermission("payments.view")]
        return [IsAuthenticated(), HasPermission("payments.manage")]

    def _get_payment(self, pk):
        try:
            return Payment.objects.select_related("venture").get(pk=pk)
        except Payment.DoesNotExist:
            return None

    def get(self, request, pk):
        payment = self._get_payment(pk)
        if payment is None:
            return failure(message="Payment not found", status=status.HTTP_404_NOT_FOUND)
        return success(PaymentSerializer(payment).data, message="Payment fetched")

    def delete(self, request, pk):
        payment = self._get_payment(pk)
        if payment is None:
            return failure(message="Payment not found", status=status.HTTP_404_NOT_FOUND)
        try:
            reverse_payment(payment)
        except ValueError as exc:
            return failure(
                message=str(exc),
                errors={"detail": [str(exc)]},
                status=status.HTTP_400_BAD_REQUEST,
            )
        payload = PaymentSerializer(payment).data
        payment.delete()
        log_audit(
            request,
            action=AuditLog.ACTION_DELETE,
            module="payments",
            object_type="Payment",
            object_id=pk,
            description=f"Reversed payment {payload['amount']} on "
            f"{payload['reference_type']} {payload['reference_id']}",
        )
        return success(None, message="Payment reversed")