from django.contrib import admin

from .models import Category, Product, Unit


@admin.register(Unit)
class UnitAdmin(admin.ModelAdmin):
    list_display = ["unit_code", "unit_name", "is_base"]
    search_fields = ["unit_code", "unit_name"]


@admin.register(Category)
class CategoryAdmin(admin.ModelAdmin):
    list_display = ["category_name", "venture", "status"]
    list_filter = ["status", "venture"]
    search_fields = ["category_name"]


@admin.register(Product)
class ProductAdmin(admin.ModelAdmin):
    list_display = ["sku", "product_name", "venture", "category", "unit", "selling_price", "status"]
    list_filter = ["status", "venture"]
    search_fields = ["sku", "product_name"]
    readonly_fields = ["sku"]