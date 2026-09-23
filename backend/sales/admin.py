from django.contrib import admin

from .models import Sale, SaleItem, SaleReturn, SaleReturnItem


class SaleItemInline(admin.TabularInline):
    model = SaleItem
    extra = 0


@admin.register(Sale)
class SaleAdmin(admin.ModelAdmin):
    list_display = [
        "invoice_number", "customer", "warehouse", "sale_date", "status",
        "total_amount", "paid_amount", "due_amount",
    ]
    list_filter = ["status", "venture"]
    search_fields = ["invoice_number", "customer__name"]
    readonly_fields = ["invoice_number", "subtotal", "total_amount", "due_amount"]
    inlines = [SaleItemInline]


@admin.register(SaleReturn)
class SaleReturnAdmin(admin.ModelAdmin):
    list_display = ["return_number", "sale", "return_date", "status", "total_amount"]
    list_filter = ["status", "venture"]
    search_fields = ["return_number", "sale__invoice_number"]
    readonly_fields = ["return_number", "total_amount"]