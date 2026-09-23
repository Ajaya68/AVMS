import { Badge } from "react-bootstrap";

import MasterEntityPage from "../../components/MasterEntityPage";
import { createUnit, deleteUnit, fetchUnits, updateUnit } from "../../services/productService";

const UNIT_FIELDS = [
  { name: "unit_code", label: "Unit code (kg, pcs, litre)", required: true },
  { name: "unit_name", label: "Unit name" },
  {
    name: "is_base",
    label: "Base unit",
    type: "select",
    options: [
      { value: "true", label: "Yes" },
      { value: "false", label: "No" },
    ],
  },
];

function UnitsPage() {
  return (
    <MasterEntityPage
      title="Units"
      subtitle="Measurement units used across the catalog"
      singular="unit"
      perm="units.manage"
      service={{
        fetch: fetchUnits,
        create: createUnit,
        update: updateUnit,
        remove: deleteUnit,
      }}
      searchPlaceholder="Search unit code or name..."
      columns={[
        { key: "unit_code", label: "Code" },
        { key: "unit_name", label: "Name" },
        {
          key: "is_base",
          label: "Base",
          render: (r) =>
            r.is_base ? <Badge bg="info">Base</Badge> : <Badge bg="light" text="dark">Derived</Badge>,
        },
      ]}
      fields={UNIT_FIELDS}
    />
  );
}

export default UnitsPage;