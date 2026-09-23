import { Badge } from "react-bootstrap";

import MasterEntityPage from "../../components/MasterEntityPage";
import {
  createCustomer,
  deleteCustomer,
  fetchCustomers,
  updateCustomer,
} from "../../services/customerService";

const CUSTOMER_FIELDS = [
  { name: "venture", label: "Venture", type: "venture", required: true },
  { name: "name", label: "Name", required: true },
  { name: "phone", label: "Phone" },
  { name: "email", label: "Email", type: "email" },
  { name: "address", label: "Address", fullWidth: true },
  { name: "city", label: "City" },
  { name: "state", label: "State" },
  { name: "pincode", label: "Pincode" },
  { name: "gst_number", label: "GST number" },
  { name: "credit_limit", label: "Credit limit", type: "number" },
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

function CustomersPage() {
  return (
    <MasterEntityPage
      title="Customers"
      subtitle="Trade accounts served across ventures"
      singular="customer"
      perm="customers.manage"
      service={{
        fetch: fetchCustomers,
        create: createCustomer,
        update: updateCustomer,
        remove: deleteCustomer,
      }}
      codeField="customer_code"
      searchPlaceholder="Search name, code, city, phone..."
      columns={[
        { key: "customer_code", label: "Code" },
        { key: "name", label: "Name" },
        { key: "phone", label: "Phone" },
        { key: "city", label: "City" },
        {
          key: "credit_limit",
          label: "Credit limit",
          render: (r) => `₹${r.credit_limit}`,
        },
        {
          key: "status",
          label: "Status",
          render: (r) => (
            <Badge bg={r.status === "ACTIVE" ? "success" : "secondary"}>{r.status}</Badge>
          ),
        },
      ]}
      fields={CUSTOMER_FIELDS}
    />
  );
}

export default CustomersPage;