from django.contrib import admin

from .models import Inventory, StockMovement, Warehouse


@admin.register(Warehouse)
class WarehouseAdmin(admin.ModelAdmin):
    list_display = ["warehouse_code", "warehouse_name", "venture", "city", "manager", "status"]
    list_filter = ["status", "venture"]
    search_fields = ["warehouse_code", "warehouse_name", "city"]
    readonly_fields = ["warehouse_code"]


@admin.register(Inventory)
class InventoryAdmin(admin.ModelAdmin):
    list_display = ["warehouse", "product", "quantity", "reserved_quantity", "reorder_level"]
    list_filter = ["warehouse", "venture"]
    search_fields = ["product__product_name", "product__sku"]


@admin.register(StockMovement)
class StockMovementAdmin(admin.ModelAdmin):
    list_display = ["movement_type", "product", "warehouse", "quantity", "movement_date", "created_by"]
    list_filter = ["movement_type", "warehouse"]
    search_fields = ["product__product_name", "product__sku", "notes"]