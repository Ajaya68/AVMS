from django.contrib import admin

from .models import Purchase, PurchaseItem, PurchaseReturn, PurchaseReturnItem


class PurchaseItemInline(admin.TabularInline):
    model = PurchaseItem
    extra = 0


@admin.register(Purchase)
class PurchaseAdmin(admin.ModelAdmin):
    list_display = [
        "invoice_number", "supplier", "warehouse", "purchase_date", "status",
        "total_amount", "paid_amount", "due_amount",
    ]
    list_filter = ["status", "venture"]
    search_fields = ["invoice_number", "supplier__name"]
    readonly_fields = ["invoice_number", "subtotal", "total_amount", "due_amount"]
    inlines = [PurchaseItemInline]


@admin.register(PurchaseReturn)
class PurchaseReturnAdmin(admin.ModelAdmin):
    list_display = ["return_number", "purchase", "return_date", "status", "total_amount"]
    list_filter = ["status", "venture"]
    search_fields = ["return_number", "purchase__invoice_number"]
    readonly_fields = ["return_number", "total_amount"]