from rest_framework import serializers

from .models import Employee


class EmployeeSerializer(serializers.ModelSerializer):
    venture_name = serializers.CharField(source="venture.venture_code", read_only=True)
    name = serializers.CharField(read_only=True)

    class Meta:
        model = Employee
        fields = [
            "id", "venture", "venture_name", "employee_code", "name",
            "first_name", "last_name", "phone", "email", "department",
            "designation", "joining_date", "salary", "status",
            "created_at", "updated_at",
        ]
        read_only_fields = ["employee_code", "created_at", "updated_at"]