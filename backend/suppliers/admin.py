from django.contrib import admin

from .models import Supplier


@admin.register(Supplier)
class SupplierAdmin(admin.ModelAdmin):
    list_display = ["supplier_code", "name", "venture", "phone", "city", "payment_terms", "status"]
    list_filter = ["status", "venture"]
    search_fields = ["name", "supplier_code", "contact_person", "phone", "email", "city"]
    readonly_fields = ["supplier_code"]