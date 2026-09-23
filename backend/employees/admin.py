from django.contrib import admin

from .models import Employee


@admin.register(Employee)
class EmployeeAdmin(admin.ModelAdmin):
    list_display = [
        "employee_code", "first_name", "last_name", "department",
        "designation", "status", "venture",
    ]
    list_filter = ["status", "department", "venture"]
    search_fields = ["first_name", "last_name", "email"]
    readonly_fields = ["employee_code"]