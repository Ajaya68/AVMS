import { useMemo } from "react";
import { Badge } from "react-bootstrap";

import { useApi } from "../../hooks/useApi";
import MasterEntityPage from "../../components/MasterEntityPage";
import {
  createProduct,
  deleteProduct,
  fetchCategories,
  fetchProducts,
  fetchUnits,
  updateProduct,
} from "../../services/productService";

const PRODUCT_FIELDS = [
  { name: "venture", label: "Venture", type: "venture", required: true },
  { name: "category", label: "Category", type: "select", lookup: "category" },
  { name: "unit", label: "Unit", type: "select", lookup: "unit", required: true },
  { name: "product_name", label: "Product name", required: true },
  { name: "description", label: "Description", fullWidth: true },
  { name: "purchase_price", label: "Purchase price", type: "number" },
  { name: "selling_price", label: "Selling price", type: "number" },
  { name: "tax_rate", label: "Tax rate %", type: "number" },
  { name: "reorder_level", label: "Reorder level", type: "number" },
  {
    name: "status",
    label: "Status",
    type: "select",
    options: [
      { value: "ACTIVE", label: "Active" },
      { value: "INACTIVE", label: "Inactive" },
    ],
  },
];

function ProductsPage() {
  const { data: categories } = useApi(() => fetchCategories({ page_size: 100 }), []);
  const { data: units } = useApi(() => fetchUnits({ page_size: 100 }), []);

  const categoryOptions = useMemo(
    () =>
      (Array.isArray(categories?.results) ? categories.results : categories || []).map(
        (c) => ({ value: c.id, label: c.category_name })
      ),
    [categories]
  );

  const unitOptions = useMemo(
    () =>
      (Array.isArray(units?.results) ? units.results : units || []).map((u) => ({
        value: u.id,
        label: u.unit_code,
      })),
    [units]
  );

  return (
    <MasterEntityPage
      title="Products"
      subtitle="Catalog of goods, services and packaging"
      singular="product"
      perm="products.manage"
      service={{
        fetch: fetchProducts,
        create: createProduct,
        update: updateProduct,
        remove: deleteProduct,
      }}
      codeField="sku"
      searchPlaceholder="Search name, SKU, category..."
      lookups={{ category: categoryOptions, unit: unitOptions }}
      columns={[
        { key: "sku", label: "SKU" },
        { key: "product_name", label: "Product" },
        { key: "category_name", label: "Category" },
        { key: "unit_code", label: "Unit" },
        {
          key: "purchase_price",
          label: "Buy",
          render: (r) => `₹${r.purchase_price}`,
        },
        {
          key: "selling_price",
          label: "Sell",
          render: (r) => `₹${r.selling_price}`,
        },
        {
          key: "status",
          label: "Status",
          render: (r) => (
            <Badge bg={r.status === "ACTIVE" ? "success" : "secondary"}>{r.status}</Badge>
          ),
        },
      ]}
      fields={PRODUCT_FIELDS}
    />
  );
}

export default ProductsPage;