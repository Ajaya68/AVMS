import MasterEntityPage from "../../components/MasterEntityPage";
import { createEmployee, deleteEmployee, fetchEmployees, updateEmployee } from "../../services/employeeService";

const FIELDS = [
  { name: "first_name", label: "First name", type: "text", required: true },
  { name: "last_name", label: "Last name", type: "text" },
  { name: "phone", label: "Phone", type: "text" },
  { name: "email", label: "Email", type: "email" },
  { name: "department", label: "Department", type: "text" },
  { name: "designation", label: "Designation", type: "text" },
  { name: "joining_date", label: "Joining date", type: "date" },
  { name: "salary", label: "Salary", type: "number" },
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

const COLUMNS = [
  { key: "employee_code", label: "Code" },
  { key: "name", label: "Name" },
  { key: "department", label: "Department" },
  { key: "designation", label: "Designation" },
  { key: "email", label: "Email" },
  { key: "phone", label: "Phone" },
  { key: "joining_date", label: "Joined" },
  { key: "salary", label: "Salary" },
  {
    key: "status",
    label: "Status",
    render: (v) => <span className={`badge ${v === "ACTIVE" ? "text-bg-success" : "text-bg-secondary"}`}>{v}</span>,
  },
];

export default function EmployeesPage() {
  return (
    <MasterEntityPage
      title="Employees"
      subtitle="Manage venture staff"
      fetchItems={fetchEmployees}
      createItem={createEmployee}
      updateItem={updateEmployee}
      deleteItem={deleteEmployee}
      fields={FIELDS}
      columns={COLUMNS}
      codeField="employee_code"
      searchable
    />
  );
}