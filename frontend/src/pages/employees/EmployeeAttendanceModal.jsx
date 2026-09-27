import { useEffect, useMemo, useState } from "react";
import { Alert, Badge, Modal, Table } from "react-bootstrap";

import LoadingSpinner from "../../components/LoadingSpinner";
import { fetchAttendance } from "../../services/employeeService";

const STATUS_TONE = {
  PRESENT: "text-bg-success",
  ABSENT: "text-bg-danger",
  HALF_DAY: "text-bg-warning",
  LEAVE: "text-bg-info",
};

const MONTHS = [
  "January", "February", "March", "April", "May", "June",
  "July", "August", "September", "October", "November", "December",
];

function monthRange(year, month) {
  const from = `${year}-${String(month).padStart(2, "0")}-01`;
  const last = new Date(year, month, 0).getDate();
  const to = `${year}-${String(month).padStart(2, "0")}-${String(last).padStart(2, "0")}`;
  return { from, to };
}

/**
 * One employee's attendance history for a chosen month, with summary
 * (working days, present/absent/leave, attendance %).
 */
export default function EmployeeAttendanceModal({ employee, show, onHide }) {
  const now = new Date();
  const [year, setYear] = useState(now.getFullYear());
  const [month, setMonth] = useState(now.getMonth() + 1);
  const [records, setRecords] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!show || !employee) return;
    const { from, to } = monthRange(year, month);
    // History reloads when the modal opens or month/year changes.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setLoading(true);
    setError("");
    fetchAttendance({ employee: employee.id, from, to, page_size: 35 })
      .then((data) => setRecords(data?.results || []))
      .catch(() => setError("Unable to load attendance history."))
      .finally(() => setLoading(false));
  }, [show, employee, year, month]);

  const summary = useMemo(() => {
    const s = { working: records.length, present: 0, absent: 0, half: 0, leave: 0 };
    for (const r of records) {
      if (r.status === "PRESENT") s.present += 1;
      else if (r.status === "ABSENT") s.absent += 1;
      else if (r.status === "HALF_DAY") s.half += 1;
      else if (r.status === "LEAVE") s.leave += 1;
    }
    s.pct = s.working > 0 ? ((s.present + s.half * 0.5) / s.working) * 100 : 0;
    return s;
  }, [records]);

  const years = [now.getFullYear() - 1, now.getFullYear(), now.getFullYear() + 1];

  return (
    <Modal show={show} onHide={onHide} size="lg">
      <Modal.Header closeButton>
        <Modal.Title>
          Attendance - {employee?.name || `${employee?.first_name || ""} ${employee?.last_name || ""}`.trim()}
          <span className="text-muted fs-6 ms-2">{employee?.employee_code}</span>
        </Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <div className="d-flex gap-2 mb-3">
          <select
            className="form-select form-select-sm"
            style={{ maxWidth: 160 }}
            value={month}
            onChange={(e) => setMonth(Number(e.target.value))}
            aria-label="Month"
          >
            {MONTHS.map((m, i) => (
              <option key={m} value={i + 1}>{m}</option>
            ))}
          </select>
          <select
            className="form-select form-select-sm"
            style={{ maxWidth: 120 }}
            value={year}
            onChange={(e) => setYear(Number(e.target.value))}
            aria-label="Year"
          >
            {years.map((y) => (
              <option key={y} value={y}>{y}</option>
            ))}
          </select>
          <div className="ms-auto d-flex gap-3 align-items-center small">
            <span><strong>{summary.working}</strong> <span className="text-muted">working days</span></span>
            <span className="text-success"><strong>{summary.present}</strong> present</span>
            <span className="text-danger"><strong>{summary.absent}</strong> absent</span>
            <span className="text-info"><strong>{summary.leave}</strong> leave</span>
            <span><strong>{summary.pct.toFixed(0)}%</strong></span>
          </div>
        </div>
        {error && <Alert variant="danger" className="py-2">{error}</Alert>}
        {loading ? (
          <LoadingSpinner label="Loading history..." />
        ) : (
          <Table size="sm" striped hover responsive className="mb-0">
            <thead>
              <tr><th>Date</th><th>Status</th><th>Check in</th><th>Check out</th><th>Remarks</th></tr>
            </thead>
            <tbody>
              {records.map((r) => (
                <tr key={r.id}>
                  <td className="text-nowrap">{r.date}</td>
                  <td>
                    <Badge bg="" className={STATUS_TONE[r.status] || "text-bg-secondary"}>
                      {r.status}
                    </Badge>
                  </td>
                  <td>{r.check_in || "-"}</td>
                  <td>{r.check_out || "-"}</td>
                  <td className="text-muted">{r.notes || "-"}</td>
                </tr>
              ))}
              {records.length === 0 && (
                <tr><td colSpan={5} className="text-center text-muted py-3">No records this month</td></tr>
              )}
            </tbody>
          </Table>
        )}
      </Modal.Body>
    </Modal>
  );
}
