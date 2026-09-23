from django.contrib import admin

from .models import Customer


@admin.register(Customer)
class CustomerAdmin(admin.ModelAdmin):
    list_display = ["customer_code", "name", "venture", "phone", "city", "credit_limit", "status"]
    list_filter = ["status", "venture"]
    search_fields = ["name", "customer_code", "phone", "email", "city"]
    readonly_fields = ["customer_code"]