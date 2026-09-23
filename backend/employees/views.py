from core.crud_views import MasterDetailView, MasterListCreateView

from .models import Employee
from .serializers import EmployeeSerializer


class EmployeeListCreateView(MasterListCreateView):
    model = Employee
    serializer_class = EmployeeSerializer
    permission_module = "employees"
    search_fields = [
        "employee_code", "first_name", "last_name", "email",
        "department", "designation",
    ]
    audit_object_type = "Employee"


class EmployeeDetailView(MasterDetailView):
    model = Employee
    serializer_class = EmployeeSerializer
    permission_module = "employees"
    audit_object_type = "Employee"