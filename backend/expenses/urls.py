from django.urls import path

from .views import (
    ExpenseCategoryListView,
    ExpenseDetailView,
    ExpenseListCreateView,
)

urlpatterns = [
    path("expenses/", ExpenseListCreateView.as_view(), name="expense-list"),
    path("expenses/<int:pk>/", ExpenseDetailView.as_view(), name="expense-detail"),
    path("expense-categories/", ExpenseCategoryListView.as_view(), name="expense-category-list"),
]