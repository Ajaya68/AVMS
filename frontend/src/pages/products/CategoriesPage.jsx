import { Badge } from "react-bootstrap";

import MasterEntityPage from "../../components/MasterEntityPage";
import {
  createCategory,
  deleteCategory,
  fetchCategories,
  updateCategory,
} from "../../services/productService";

const CATEGORY_FIELDS = [
  { name: "venture", label: "Venture", type: "venture", required: true },
  { name: "category_name", label: "Category name", required: true },
  { name: "description", label: "Description", fullWidth: true },
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

function CategoriesPage() {
  return (
    <MasterEntityPage
      title="Categories"
      subtitle="Product groupings for reporting and pricing"
      singular="category"
      perm="categories.manage"
      service={{
        fetch: fetchCategories,
        create: createCategory,
        update: updateCategory,
        remove: deleteCategory,
      }}
      searchPlaceholder="Search category name..."
      columns={[
        { key: "category_name", label: "Category" },
        { key: "description", label: "Description" },
        {
          key: "status",
          label: "Status",
          render: (r) => (
            <Badge bg={r.status === "ACTIVE" ? "success" : "secondary"}>{r.status}</Badge>
          ),
        },
      ]}
      fields={CATEGORY_FIELDS}
    />
  );
}

export default CategoriesPage;