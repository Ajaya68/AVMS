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

from .models import Expense, ExpenseCategory
from .serializers import ExpenseCategorySerializer, ExpenseSerializer


class ExpenseCategoryListView(APIView):
    def get_permissions(self):
        return [IsAuthenticated(), HasPermission("expenses.view")]

    def get(self, request):
        return success(
            ExpenseCategorySerializer(ExpenseCategory.objects.all(), many=True).data,
            message="Expense categories fetched",
        )


class ExpenseListCreateView(APIView):
    pagination_class = PageNumberPagination

    def get_permissions(self):
        code = "expenses.manage" if self.request.method == "POST" else "expenses.view"
        return [IsAuthenticated(), HasPermission(code)]

    def get(self, request):
        queryset = Expense.objects.select_related("venture", "category").all()
        queryset = scope_queryset_by_venture(queryset, request)

        term = request.query_params.get("search", "").strip()
        if term:
            queryset = queryset.filter(
                Q(description__icontains=term) | Q(category__category_name__icontains=term)
            )

        category = request.query_params.get("category", "").strip()
        if category:
            queryset = queryset.filter(category_id=category)

        paginator = self.pagination_class()
        page = paginator.paginate_queryset(queryset, request)
        data = paginator.get_paginated_response(
            ExpenseSerializer(page, many=True).data
        ).data
        return success(data, message="Expenses fetched")

    def post(self, request):
        serializer = ExpenseSerializer(data=request.data, context={"request": request})
        if not serializer.is_valid():
            return failure(
                message="Unable to create expense",
                errors=serializer.errors,
                status=status.HTTP_400_BAD_REQUEST,
            )
        expense = serializer.save()
        log_audit(
            request,
            action=AuditLog.ACTION_CREATE,
            module="expenses",
            object_type="Expense",
            object_id=expense.id,
            description=f"Expense {expense.amount} on {expense.category.category_name}",
        )
        return success(
            ExpenseSerializer(expense).data,
            message="Expense recorded",
            status=status.HTTP_201_CREATED,
        )


class ExpenseDetailView(APIView):
    def get_permissions(self):
        if self.request.method == "GET":
            return [IsAuthenticated(), HasPermission("expenses.view")]
        return [IsAuthenticated(), HasPermission("expenses.manage")]

    def _get_expense(self, pk):
        try:
            return scope_queryset_by_venture(
                Expense.objects.select_related("venture", "category"), self.request
            ).get(pk=pk)
        except Expense.DoesNotExist:
            return None

    def get(self, request, pk):
        expense = self._get_expense(pk)
        if expense is None:
            return failure(message="Expense not found", status=status.HTTP_404_NOT_FOUND)
        return success(ExpenseSerializer(expense).data, message="Expense fetched")

    def patch(self, request, pk):
        expense = self._get_expense(pk)
        if expense is None:
            return failure(message="Expense not found", status=status.HTTP_404_NOT_FOUND)
        serializer = ExpenseSerializer(expense, data=request.data, partial=True)
        if not serializer.is_valid():
            return failure(
                message="Unable to update expense",
                errors=serializer.errors,
                status=status.HTTP_400_BAD_REQUEST,
            )
        expense = serializer.save()
        log_audit(
            request,
            action=AuditLog.ACTION_UPDATE,
            module="expenses",
            object_type="Expense",
            object_id=expense.id,
            description=f"Updated expense {expense.id}",
        )
        return success(ExpenseSerializer(expense).data, message="Expense updated")

    def delete(self, request, pk):
        expense = self._get_expense(pk)
        if expense is None:
            return failure(message="Expense not found", status=status.HTTP_404_NOT_FOUND)
        amount = expense.amount
        expense.delete()
        log_audit(
            request,
            action=AuditLog.ACTION_DELETE,
            module="expenses",
            object_type="Expense",
            object_id=pk,
            description=f"Deleted expense {amount}",
        )
        return success(None, message="Expense deleted")