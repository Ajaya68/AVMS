from core.crud_views import MasterDetailView, MasterListCreateView

from .models import Category, Product, Unit
from .serializers import CategorySerializer, ProductSerializer, UnitSerializer


class UnitListCreateView(MasterListCreateView):
    model = Unit
    serializer_class = UnitSerializer
    permission_module = "units"
    search_fields = ["unit_code", "unit_name"]
    scope_by_venture = False
    audit_object_type = "Unit"


class UnitDetailView(MasterDetailView):
    model = Unit
    serializer_class = UnitSerializer
    permission_module = "units"
    audit_object_type = "Unit"


class CategoryListCreateView(MasterListCreateView):
    model = Category
    serializer_class = CategorySerializer
    permission_module = "categories"
    search_fields = ["category_name"]
    audit_object_type = "Category"


class CategoryDetailView(MasterDetailView):
    model = Category
    serializer_class = CategorySerializer
    permission_module = "categories"
    audit_object_type = "Category"


class ProductListCreateView(MasterListCreateView):
    model = Product
    serializer_class = ProductSerializer
    permission_module = "products"
    search_fields = ["product_name", "sku", "category__category_name"]
    audit_object_type = "Product"


class ProductDetailView(MasterDetailView):
    model = Product
    serializer_class = ProductSerializer
    permission_module = "products"
    audit_object_type = "Product"