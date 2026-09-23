from core.crud_views import MasterDetailView, MasterListCreateView

from .models import Customer
from .serializers import CustomerSerializer


class CustomerListCreateView(MasterListCreateView):
    model = Customer
    serializer_class = CustomerSerializer
    permission_module = "customers"
    search_fields = ["name", "customer_code", "city", "phone", "email"]
    audit_object_type = "Customer"


class CustomerDetailView(MasterDetailView):
    model = Customer
    serializer_class = CustomerSerializer
    permission_module = "customers"
    audit_object_type = "Customer"