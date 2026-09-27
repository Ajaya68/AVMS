import { useMemo, useState } from "react";
import { Button, Card, Col, Form, Row } from "react-bootstrap";

import MasterEntityPage from "../../components/MasterEntityPage";
import { useAuth } from "../../context/AuthContext";
import { useApi } from "../../hooks/useApi";
import {
  DEPARTMENT_OPTIONS,
  DESIGNATION_OPTIONS,
} from "../../utils/employeeOptions";
import {
  createEmployee,
  deleteEmployee,
  fetchAttendance,
  fetchEmployees,
  updateEmployee,
} from "../../services/employeeService";
import EmployeeAttendanceModal from "./EmployeeAttendanceModal";

// Master-data only: venture, identity, contacts, department, designation,
// joining, salary (numeric), employment status. No login linking here (it
// lives with user management / automatic email match) and no attendance
// controls (those live on the Attendance page and history modal).
const FIELDS = [
  { name: "venture", label: "Venture", type: "venture", required: true },
  { name: "first_name", label: "First name", type: "text", required: true },
  { name: "last_name", label: "Last name", type: "text" },
  { name: "phone", label: "Phone", type: "text" },
  { name: "email", label: "Email", type: "email" },
  { name: "department", label: "Department", type: "datalist", options: DEPARTMENT_OPTIONS },
  { name: "designation", label: "Designation", type: "datalist", options: DESIGNATION_OPTIONS },
  { name: "joining_date", label: "Joining date", type: "date" },
  { name: "salary", label: "Salary (₹ / month)", type: "number" },
  {
    name: "status",
    label: "Employment status",
    type: "select",
    options: [
      { value: "ACTIVE", label: "Active" },
      { value: "INACTIVE", label: "Inactive" },
      { value: "TERMINATED", label: "Terminated" },
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
    render: (row) => <span className={`badge ${row.status === "ACTIVE" ? "text-bg-success" : row.status === "TERMINATED" ? "text-bg-danger" : "text-bg-secondary"}`}>{row.status}</span>,
  },
];

// Banner gradients, picked per department so cards are visually distinct
// like product tiles on a storefront.
const BANNERS = [
  "linear-gradient(135deg, #4f46e5, #9333ea)",
  "linear-gradient(135deg, #0ea5e9, #10b981)",
  "linear-gradient(135deg, #f59e0b, #ef4444)",
  "linear-gradient(135deg, #ec4899, #8b5cf6)",
  "linear-gradient(135deg, #14b8a6, #3b82f6)",
  "linear-gradient(135deg, #f97316, #eab308)",
];

const bannerFor = (key) => {
  let hash = 0;
  for (const ch of String(key || "?")) hash = (hash * 31 + ch.charCodeAt(0)) >>> 0;
  return BANNERS[hash % BANNERS.length];
};

const moneyIN = (v) =>
  `₹${Number(v || 0).toLocaleString("en-IN", { maximumFractionDigits: 2 })}`;

function ContactRow({ icon, value }) {
  if (!value) return null;
  return (
    <div className="d-flex align-items-center gap-2 small text-muted mb-1">
      <i className={`bi ${icon}`} />
      <span className="text-truncate">{value}</span>
    </div>
  );
}

function Stat({ value, label, tone }) {
  return (
    <div className="text-center flex-fill">
      <div className={`fs-5 fw-bold ${tone || ""}`}>{value}</div>
      <div className="text-muted" style={{ fontSize: "0.7rem" }}>{label}</div>
    </div>
  );
}

function EmployeeCard(monthCounts, onViewAttendance) {
  return function EmployeeTile(row, { onEdit, onDelete, canManage }) {
    const name = row.name || `${row.first_name || ""} ${row.last_name || ""}`.trim() || "—";
    const initials =
      `${row.first_name?.charAt(0) || ""}${row.last_name?.charAt(0) || ""}`.toUpperCase() || "•";
    const stats = monthCounts[row.id] || { PRESENT: 0, ABSENT: 0, LEAVE: 0 };
    return (
      <Col xs={12} sm={6} xl={4} key={row.id}>
        <Card className="shadow-sm h-100 overflow-hidden">
          <div
            className="position-relative"
            style={{ height: 88, background: bannerFor(row.department) }}
          >
            <span className="position-absolute top-0 start-0 m-2 badge bg-dark bg-opacity-50">
              {row.employee_code}
            </span>
            <span className={`position-absolute top-0 end-0 m-2 badge ${row.status === "ACTIVE" ? "text-bg-success" : row.status === "TERMINATED" ? "text-bg-danger" : "text-bg-secondary"}`}>
              {row.status}
            </span>
          </div>
          <Card.Body className="pt-0">
            <div
              className="rounded-circle text-white d-flex align-items-center justify-content-center fw-bold border border-3 border-white shadow-sm"
              style={{
                width: 56,
                height: 56,
                fontSize: "1.25rem",
                background: bannerFor(row.employee_code),
                marginTop: -28,
              }}
            >
              {initials}
            </div>
            <div className="fw-semibold fs-5 mt-2 text-truncate">{name}</div>
            <div className="text-muted small mb-3">
              {[row.designation, row.department].filter(Boolean).join(" · ") || "—"}
            </div>
            <div className="d-flex gap-2 bg-light rounded-3 px-2 py-2 mb-3">
              <Stat value={stats.PRESENT} label="Present" tone="text-success" />
              <Stat value={stats.ABSENT} label="Absent" tone="text-danger" />
              <Stat value={stats.LEAVE} label="Leave" tone="text-info" />
            </div>
            <ContactRow icon="bi-envelope" value={row.email} />
            <ContactRow icon="bi-telephone" value={row.phone} />
            <ContactRow icon="bi-calendar-event" value={row.joining_date ? `Joined ${row.joining_date}` : null} />
            <div className="d-flex align-items-baseline gap-1 mt-2">
              <span className="fs-5 fw-bold text-primary">{moneyIN(row.salary)}</span>
              <span className="text-muted small">/ month</span>
            </div>
          </Card.Body>
          <Card.Footer className="bg-white d-flex gap-2">
            <Button size="sm" variant="outline-info" className="flex-fill" onClick={() => onViewAttendance(row)}>
              <i className="bi bi-calendar-check me-1" />
              View Attendance
            </Button>
            {canManage && (
              <>
                <Button size="sm" variant="outline-secondary" className="flex-fill" onClick={() => onEdit(row)}>
                  <i className="bi bi-pencil me-1" />
                  Edit
                </Button>
                <Button size="sm" variant="outline-danger" className="flex-fill" onClick={() => onDelete(row)}>
                  <i className="bi bi-trash me-1" />
                  Delete
                </Button>
              </>
            )}
          </Card.Footer>
        </Card>
      </Col>
    );
  };
}

export default function EmployeesPage() {
  const { hasPerm } = useAuth();
  const canViewAttendance = hasPerm("attendance.view");
  const [deptFilter, setDeptFilter] = useState("");
  const [viewing, setViewing] = useState(null);

  const monthStart = useMemo(() => {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-01`;
  }, []);
  const { data: attendanceData } = useApi(
    () =>
      canViewAttendance
        ? fetchAttendance({ from: monthStart, page_size: 200 })
        : Promise.resolve([]),
    [canViewAttendance, monthStart]
  );

  const monthCounts = useMemo(() => {
    const map = {};
    const list = attendanceData?.results || [];
    for (const r of list) {
      const entry = (map[r.employee] = map[r.employee] || { PRESENT: 0, ABSENT: 0, LEAVE: 0 });
      if (entry[r.status] !== undefined) entry[r.status] += 1;
    }
    return map;
  }, [attendanceData]);

  return (
    <div>
      <Row className="g-2 mb-3 align-items-end">
        <Col md={4} className="ms-auto">
          <Form.Select
            size="sm"
            value={deptFilter}
            onChange={(e) => setDeptFilter(e.target.value)}
            aria-label="Filter by department"
          >
            <option value="">All departments</option>
            {DEPARTMENT_OPTIONS.map((d) => (
              <option key={d} value={d}>{d}</option>
            ))}
          </Form.Select>
        </Col>
      </Row>
      <MasterEntityPage
        title="Employees"
        subtitle="Manage venture staff"
        singular="employee"
        perm="employees.manage"
        service={{
          fetch: fetchEmployees,
          create: createEmployee,
          update: updateEmployee,
          remove: deleteEmployee,
        }}
        fields={FIELDS}
        columns={COLUMNS}
        codeField="employee_code"
        cardRender={EmployeeCard(monthCounts, setViewing)}
        baseParams={deptFilter ? { department: deptFilter } : undefined}
      />
      <EmployeeAttendanceModal
        employee={viewing}
        show={viewing !== null}
        onHide={() => setViewing(null)}
      />
    </div>
  );
}
