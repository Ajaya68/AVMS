from django.contrib import admin

from .models import Expense, ExpenseCategory


@admin.register(ExpenseCategory)
class ExpenseCategoryAdmin(admin.ModelAdmin):
    list_display = ["category_code", "category_name"]
    search_fields = ["category_name"]


@admin.register(Expense)
class ExpenseAdmin(admin.ModelAdmin):
    list_display = ["expense_date", "category", "amount", "payment_method", "venture"]
    list_filter = ["category", "payment_method", "venture"]
    search_fields = ["description"]