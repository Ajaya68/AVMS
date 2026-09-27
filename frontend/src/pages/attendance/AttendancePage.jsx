import { useCallback, useEffect, useMemo, useState } from "react";
import {
  Alert,
  Badge,
  Button,
  Card,
  Col,
  Form,
  Row,
  Table,
} from "react-bootstrap";

import LoadingSpinner from "../../components/LoadingSpinner";
import { useAuth } from "../../context/AuthContext";
import { useApi } from "../../hooks/useApi";
import {
  bulkAttendance,
  fetchAttendance,
  fetchEmployees,
} from "../../services/employeeService";
import { fetchVentures } from "../../services/ventureService";
import { DEPARTMENT_OPTIONS } from "../../utils/employeeOptions";

const DAY_STATUSES = ["PRESENT", "ABSENT", "LEAVE"];
const STATUS_TONE = {
  PRESENT: "success",
  ABSENT: "danger",
  LEAVE: "info",
};
const todayISO = () => new Date().toISOString().slice(0, 10);

/**
 * Daily attendance roster: pick date / venture / department, see every
 * employee with quick Present / Absent / Leave buttons, optional remarks,
 * "Mark All Present", then one Save. Saving upserts, so re-saving a day
 * updates records instead of duplicating them.
 */
export default function AttendancePage() {
  const { hasPerm } = useAuth();
  const canManage = hasPerm("attendance.manage");
  const canViewVentures = hasPerm("ventures.view");
  const canViewEmployees = hasPerm("employees.view");

  const [date, setDate] = useState(todayISO());
  const [ventureId, setVentureId] = useState("");
  const [department, setDepartment] = useState("");
  const [employeeId, setEmployeeId] = useState("");
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const { data: venturesData } = useApi(
    () => (canViewVentures ? fetchVentures({ page_size: 100 }) : Promise.resolve([])),
    [canViewVentures]
  );
  const ventures = useMemo(() => {
    const list = venturesData?.results || venturesData || [];
    return Array.isArray(list) ? list : [];
  }, [venturesData]);

  const { data: employeesData } = useApi(
    () => (canViewEmployees ? fetchEmployees({ page_size: 200 }) : Promise.resolve([])),
    [canViewEmployees]
  );
  const allEmployees = useMemo(() => {
    const list = employeesData?.results || employeesData || [];
    return Array.isArray(list) ? list : [];
  }, [employeesData]);

  const loadRoster = useCallback(async () => {
    setLoading(true);
    setError("");
    setMessage("");
    try {
      const empParams = { page_size: 200 };
      if (ventureId) empParams.venture = ventureId;
      if (department) empParams.department = department;
      if (employeeId) empParams.search = undefined;
      const empData = await fetchEmployees(empParams);
      let list = empData?.results || [];
      if (employeeId) list = list.filter((e) => String(e.id) === String(employeeId));

      const attParams = { from: date, to: date, page_size: 200 };
      if (ventureId) attParams.venture = ventureId;
      if (department) attParams.department = department;
      if (employeeId) attParams.employee = employeeId;
      const attData = await fetchAttendance(attParams);
      const existing = {};
      for (const r of attData?.results || []) existing[r.employee] = r;

      setRows(
        list.map((e) => ({
          employee: e.id,
          code: e.employee_code,
          name: e.name,
          department: e.department,
          status: existing[e.id]?.status || "PRESENT",
          remarks: existing[e.id]?.notes || "",
          saved: Boolean(existing[e.id]),
        }))
      );
    } catch (e) {
      setError(e?.response?.data?.message || "Failed to load roster");
    } finally {
      setLoading(false);
    }
  }, [date, ventureId, department, employeeId]);

  useEffect(() => {
    // Roster reloads on every filter change; loadRoster is stable via useCallback.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadRoster();
  }, [loadRoster]);

  const setRowStatus = (id, status) =>
    setRows((rs) => rs.map((r) => (r.employee === id ? { ...r, status } : r)));
  const setRowRemarks = (id, remarks) =>
    setRows((rs) => rs.map((r) => (r.employee === id ? { ...r, remarks } : r)));
  const markAllPresent = () =>
    setRows((rs) => rs.map((r) => ({ ...r, status: "PRESENT" })));

  const handleSave = async () => {
    setSaving(true);
    setMessage("");
    setError("");
    try {
      const res = await bulkAttendance({
        date,
        items: rows.map((r) => ({
          employee: r.employee,
          status: r.status,
          remarks: r.remarks,
        })),
      });
      setMessage(res?.message || `Attendance saved for ${date}.`);
      await loadRoster();
    } catch (e) {
      const errors = e?.response?.data?.errors || {};
      const first = Object.values(errors).flat()[0];
      setError(first || e?.response?.data?.message || "Unable to save attendance.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div>
      <div className="d-flex flex-wrap align-items-center justify-content-between mb-4">
        <div>
          <h4 className="mb-1">Attendance</h4>
          <p className="text-muted mb-0">Mark daily presence across the team</p>
        </div>
        {canManage && rows.length > 0 && (
          <div className="d-flex gap-2">
            <Button variant="outline-success" size="sm" onClick={markAllPresent}>
              <i className="bi bi-check-all me-1" />
              Mark All Present
            </Button>
            <Button variant="primary" size="sm" disabled={saving} onClick={handleSave}>
              {saving ? "Saving..." : "Save Attendance"}
            </Button>
          </div>
        )}
      </div>

      <Card className="shadow-sm mb-3">
        <Card.Body>
          <Row className="g-2 align-items-end">
            <Col md={2}>
              <Form.Label className="small text-muted mb-0">Date</Form.Label>
              <Form.Control type="date" value={date} onChange={(e) => setDate(e.target.value)} />
            </Col>
            {canViewVentures && (
              <Col md={3}>
                <Form.Label className="small text-muted mb-0">Venture</Form.Label>
                <Form.Select value={ventureId} onChange={(e) => setVentureId(e.target.value)}>
                  <option value="">All ventures</option>
                  {ventures.map((v) => (
                    <option key={v.id} value={v.id}>
                      {v.venture_code} - {v.venture_name}
                    </option>
                  ))}
                </Form.Select>
              </Col>
            )}
            <Col md={3}>
              <Form.Label className="small text-muted mb-0">Department</Form.Label>
              <Form.Select value={department} onChange={(e) => setDepartment(e.target.value)}>
                <option value="">All departments</option>
                {DEPARTMENT_OPTIONS.map((d) => (
                  <option key={d} value={d}>{d}</option>
                ))}
              </Form.Select>
            </Col>
            {canViewEmployees && (
              <Col md={3}>
                <Form.Label className="small text-muted mb-0">Employee</Form.Label>
                <Form.Select value={employeeId} onChange={(e) => setEmployeeId(e.target.value)}>
                  <option value="">All employees</option>
                  {allEmployees.map((e) => (
                    <option key={e.id} value={e.id}>
                      {e.employee_code} - {e.name}
                    </option>
                  ))}
                </Form.Select>
              </Col>
            )}
          </Row>
        </Card.Body>
      </Card>

      {message && <Alert variant="success" className="py-2">{message}</Alert>}
      {error && <Alert variant="danger" className="py-2">{error}</Alert>}

      <Card className="shadow-sm">
        <Card.Body>
          {loading ? (
            <LoadingSpinner label="Loading roster..." />
          ) : rows.length === 0 ? (
            <div className="text-center text-muted py-4">No employees match these filters</div>
          ) : (
            <Table hover responsive className="mb-0 align-middle">
              <thead>
                <tr>
                  <th>Employee</th>
                  <th>Status</th>
                  <th>Remarks</th>
                  <th className="text-end">Saved</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((r) => (
                  <tr key={r.employee}>
                    <td>
                      <div className="fw-semibold">{r.name}</div>
                      <small className="text-muted">{r.code}{r.department ? ` · ${r.department}` : ""}</small>
                    </td>
                    <td>
                      {canManage ? (
                        <div className="btn-group btn-group-sm" role="group" aria-label={`Mark ${r.name}`}>
                          {DAY_STATUSES.map((s) => (
                            <Button
                              key={s}
                              variant={r.status === s ? STATUS_TONE[s] : "outline-secondary"}
                              onClick={() => setRowStatus(r.employee, s)}
                            >
                              {s === "PRESENT" ? "Present" : s === "ABSENT" ? "Absent" : "Leave"}
                            </Button>
                          ))}
                        </div>
                      ) : (
                        <Badge bg="" className={r.status === "PRESENT" ? "text-bg-success" : r.status === "ABSENT" ? "text-bg-danger" : "text-bg-info"}>
                          {r.status}
                        </Badge>
                      )}
                    </td>
                    <td style={{ minWidth: 160 }}>
                      {canManage ? (
                        <Form.Control
                          size="sm"
                          placeholder="Reason (for leave)"
                          value={r.remarks}
                          onChange={(e) => setRowRemarks(r.employee, e.target.value)}
                        />
                      ) : (
                        <span className="text-muted small">{r.remarks || "-"}</span>
                      )}
                    </td>
                    <td className="text-end">
                      {r.saved ? (
                        <i className="bi bi-check-circle text-success" title="Already saved" />
                      ) : (
                        <span className="text-muted small">—</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </Table>
          )}
        </Card.Body>
      </Card>
    </div>
  );
}
