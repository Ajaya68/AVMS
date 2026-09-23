from core.crud_views import MasterDetailView, MasterListCreateView

from .models import Supplier
from .serializers import SupplierSerializer


class SupplierListCreateView(MasterListCreateView):
    model = Supplier
    serializer_class = SupplierSerializer
    permission_module = "suppliers"
    search_fields = ["name", "supplier_code", "contact_person", "city", "phone", "email"]
    audit_object_type = "Supplier"


class SupplierDetailView(MasterDetailView):
    model = Supplier
    serializer_class = SupplierSerializer
    permission_module = "suppliers"
    audit_object_type = "Supplier"