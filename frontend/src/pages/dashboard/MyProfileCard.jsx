import { useState } from "react";
import { Alert, Badge, Button, Card, Col, Row } from "react-bootstrap";

import LoadingSpinner from "../../components/LoadingSpinner";
import { useApi } from "../../hooks/useApi";
import {
  fetchMyProfile,
  markAttendance,
  updateAttendance,
} from "../../services/employeeService";

const STATUS_TONE = {
  PRESENT: "text-bg-success",
  ABSENT: "text-bg-danger",
  HALF_DAY: "text-bg-warning",
  LEAVE: "text-bg-info",
};

/**
 * Self-service card for the signed-in employee: own HR record, this
 * month's attendance counts, today's status and check in/out buttons.
 * Rendered only when a login is linked to an employee record.
 */
function MyProfileCard() {
  const { data, loading, error, refetch } = useApi(() => fetchMyProfile(), []);
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState("");

  if (loading) return <LoadingSpinner label="Loading your profile..." />;
  if (error || !data?.linked) return null;

  const employee = data.employee || {};
  const today = data.today || null;
  const summary = data.month_summary || {};

  const act = async (fn, okMessage) => {
    setBusy(true);
    setNotice("");
    try {
      await fn();
      setNotice(okMessage);
      await refetch();
    } catch (err) {
      setNotice(
        err?.response?.data?.message || "Unable to record attendance."
      );
    } finally {
      setBusy(false);
    }
  };

  const checkIn = () => act(() => markAttendance({}), "Checked in. Have a good day!");
  const checkOut = () =>
    act(() => updateAttendance(today.id, { check_out: "now" }), "Checked out.");

  return (
    <Card className="shadow-sm mb-4">
      <Card.Header className="bg-white d-flex justify-content-between align-items-center">
        <strong>
          <i className="bi bi-person-badge me-2" />
          My Profile
        </strong>
        {today ? (
          <Badge bg="" className={STATUS_TONE[today.status] || "text-bg-secondary"}>
            Today: {today.status}
            {today.check_in ? ` · in ${today.check_in}` : ""}
            {today.check_out ? ` · out ${today.check_out}` : ""}
          </Badge>
        ) : (
          <Badge bg="" className="text-bg-secondary">Today: not marked</Badge>
        )}
      </Card.Header>
      <Card.Body>
        {notice && (
          <Alert variant="info" className="py-2" dismissible onClose={() => setNotice("")}>
            {notice}
          </Alert>
        )}
        <Row className="g-3 align-items-center">
          <Col md={6}>
            <div className="fw-semibold fs-5">
              {employee.name || `${employee.first_name || ""} ${employee.last_name || ""}`.trim() || "-"}
              <span className="text-muted small ms-2">{employee.employee_code}</span>
            </div>
            <div className="text-muted small">
              {[employee.designation, employee.department].filter(Boolean).join(" · ") || "-"}
            </div>
            <div className="text-muted small">
              Joined {employee.joining_date || "-"}
              <span className={`ms-2 badge ${employee.status === "ACTIVE" ? "text-bg-success" : "text-bg-secondary"}`}>
                {employee.status}
              </span>
            </div>
          </Col>
          <Col md={3}>
            <div className="d-flex gap-3 text-center">
              <div>
                <div className="fs-5 fw-bold text-success">{summary.present ?? 0}</div>
                <div className="text-muted small">Present</div>
              </div>
              <div>
                <div className="fs-5 fw-bold text-danger">{summary.absent ?? 0}</div>
                <div className="text-muted small">Absent</div>
              </div>
              <div>
                <div className="fs-5 fw-bold text-info">{summary.leave ?? 0}</div>
                <div className="text-muted small">Leave</div>
              </div>
            </div>
            <div className="text-muted small mt-1">This month</div>
          </Col>
          <Col md={3} className="text-md-end">
            {!today && (
              <Button variant="success" disabled={busy} onClick={checkIn}>
                <i className="bi bi-box-arrow-in-right me-2" />
                {busy ? "Please wait..." : "Check in"}
              </Button>
            )}
            {today && !today.check_out && (
              <Button variant="outline-primary" disabled={busy} onClick={checkOut}>
                <i className="bi bi-box-arrow-right me-2" />
                {busy ? "Please wait..." : "Check out"}
              </Button>
            )}
            {today?.check_out && (
              <span className="text-muted small">Checked out at {today.check_out}</span>
            )}
          </Col>
        </Row>
      </Card.Body>
    </Card>
  );
}

export default MyProfileCard;
