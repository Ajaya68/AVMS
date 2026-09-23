import { Badge } from "react-bootstrap";

import MasterEntityPage from "../../components/MasterEntityPage";
import {
  createWarehouse,
  deleteWarehouse,
  fetchWarehouses,
  updateWarehouse,
} from "../../services/inventoryService";

const WAREHOUSE_FIELDS = [
  { name: "venture", label: "Venture", type: "venture", required: true },
  { name: "warehouse_name", label: "Warehouse name", required: true },
  { name: "address", label: "Address", fullWidth: true },
  { name: "city", label: "City" },
  { name: "state", label: "State" },
  { name: "pincode", label: "Pincode" },
  { name: "manager", label: "Manager" },
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

function WarehousesPage() {
  return (
    <MasterEntityPage
      title="Warehouses"
      subtitle="Storage locations where stock is held"
      singular="warehouse"
      perm="warehouses.manage"
      service={{
        fetch: fetchWarehouses,
        create: createWarehouse,
        update: updateWarehouse,
        remove: deleteWarehouse,
      }}
      codeField="warehouse_code"
      searchPlaceholder="Search name, code, city, manager..."
      columns={[
        { key: "warehouse_code", label: "Code" },
        { key: "warehouse_name", label: "Warehouse" },
        { key: "city", label: "City" },
        { key: "manager", label: "Manager" },
        {
          key: "status",
          label: "Status",
          render: (r) => (
            <Badge bg={r.status === "ACTIVE" ? "success" : "secondary"}>{r.status}</Badge>
          ),
        },
      ]}
      fields={WAREHOUSE_FIELDS}
    />
  );
}

export default WarehousesPage;