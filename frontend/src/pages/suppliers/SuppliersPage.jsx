import { Badge } from "react-bootstrap";

import MasterEntityPage from "../../components/MasterEntityPage";
import {
  createSupplier,
  deleteSupplier,
  fetchSuppliers,
  updateSupplier,
} from "../../services/supplierService";

const SUPPLIER_FIELDS = [
  { name: "venture", label: "Venture", type: "venture", required: true },
  { name: "name", label: "Name", required: true },
  { name: "contact_person", label: "Contact person" },
  { name: "phone", label: "Phone" },
  { name: "email", label: "Email", type: "email" },
  { name: "address", label: "Address", fullWidth: true },
  { name: "city", label: "City" },
  { name: "state", label: "State" },
  { name: "pincode", label: "Pincode" },
  { name: "gst_number", label: "GST number" },
  { name: "payment_terms", label: "Payment terms" },
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

function SuppliersPage() {
  return (
    <MasterEntityPage
      title="Suppliers"
      subtitle="Vendors supplying raw material and goods"
      singular="supplier"
      perm="suppliers.manage"
      service={{
        fetch: fetchSuppliers,
        create: createSupplier,
        update: updateSupplier,
        remove: deleteSupplier,
      }}
      codeField="supplier_code"
      searchPlaceholder="Search name, code, city, phone..."
      columns={[
        { key: "supplier_code", label: "Code" },
        { key: "name", label: "Name" },
        { key: "contact_person", label: "Contact" },
        { key: "phone", label: "Phone" },
        { key: "city", label: "City" },
        { key: "payment_terms", label: "Payment terms" },
        {
          key: "status",
          label: "Status",
          render: (r) => (
            <Badge bg={r.status === "ACTIVE" ? "success" : "secondary"}>{r.status}</Badge>
          ),
        },
      ]}
      fields={SUPPLIER_FIELDS}
    />
  );
}

export default SuppliersPage;