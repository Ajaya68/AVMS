import { useMemo, useState } from "react";
import { Alert, Badge, Button, Card } from "react-bootstrap";
import { Link } from "react-router-dom";

import LoadingSpinner from "../../components/LoadingSpinner";
import { useApi } from "../../hooks/useApi";
import { fetchAttendance, fetchMyProfile } from "../../services/employeeService";

const DAY_TONE = {
  PRESENT: "success",
  ABSENT: "danger",
  HALF_DAY: "warning",
  LEAVE: "info",
};

const monthKeyOf = (date) =>
  `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}`;

const monthLabelOf = (date) =>
  date.toLocaleString(undefined, { month: "long", year: "numeric" });

/**
 * Employee attendance dashboard panel: month navigator, present/absent/
 * leave counts and a calendar grid of the signed-in employee's own
 * attendance. Renders nothing unless the login is linked to an employee
 * record (same rule as MyProfileCard).
 */
function MyAttendancePanel() {
  const [offset, setOffset] = useState(0);
  const monthDate = useMemo(() => {
    const now = new Date();
    return new Date(now.getFullYear(), now.getMonth() + offset, 1);
  }, [offset]);
  const monthKey = monthKeyOf(monthDate);

  const { data: profile, loading: profileLoading } = useApi(
    () => fetchMyProfile(),
    []
  );
  const employeeId = profile?.linked ? profile?.employee?.id : null;

  const year = monthDate.getFullYear();
  const month = monthDate.getMonth();
  const from = `${monthKey}-01`;
  const lastDay = new Date(year, month + 1, 0).getDate();
  const to = `${monthKey}-${String(lastDay).padStart(2, "0")}`;

  const {
    data: attData,
    loading: attLoading,
    error: attError,
  } = useApi(
    () =>
      employeeId
        ? fetchAttendance({ employee: employeeId, from, to, page_size: 100 })
        : Promise.resolve(null),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [employeeId, monthKey]
  );

  const byDate = useMemo(() => {
    const map = {};
    const list = attData?.results || [];
    for (const r of list) {
      if (r.date) map[r.date] = r;
    }
    return map;
  }, [attData]);

  const counts = useMemo(() => {
    const c = { PRESENT: 0, ABSENT: 0, HALF_DAY: 0, LEAVE: 0 };
    for (const r of Object.values(byDate)) {
      if (c[r.status] !== undefined) c[r.status] += 1;
    }
    return c;
  }, [byDate]);

  if (profileLoading) return <LoadingSpinner label="Loading your attendance..." />;
  if (!employeeId) return null;

  const firstWeekday = new Date(year, month, 1).getDay();
  const cells = [
    ...Array(firstWeekday).fill(null),
    ...Array.from({ length: lastDay }, (_, i) => i + 1),
  ];
  const todayISO = new Date().toISOString().slice(0, 10);

  return (
    <Card className="shadow-sm mb-4">
      <Card.Header className="bg-white d-flex flex-wrap justify-content-between align-items-center gap-2">
        <strong>
          <i className="bi bi-calendar-check me-2" />
          My Attendance
        </strong>
        <div className="d-flex align-items-center gap-2">
          <Button
            size="sm"
            variant="outline-secondary"
            onClick={() => setOffset((o) => o - 1)}
            aria-label="Previous month"
          >
            <i className="bi bi-chevron-left" />
          </Button>
          <span className="fw-semibold small" style={{ minWidth: 120, textAlign: "center" }}>
            {monthLabelOf(monthDate)}
          </span>
          <Button
            size="sm"
            variant="outline-secondary"
            onClick={() => setOffset((o) => o + 1)}
            disabled={offset >= 0}
            aria-label="Next month"
          >
            <i className="bi bi-chevron-right" />
          </Button>
          <Link to="/attendance" className="small text-decoration-none ms-2">
            Full view
          </Link>
        </div>
      </Card.Header>
      <Card.Body>
        {attLoading ? (
          <LoadingSpinner label="Loading month..." />
        ) : attError ? (
          <Alert variant="danger" className="mb-0">
            Unable to load attendance for this month.
          </Alert>
        ) : (
          <>
            <div className="d-flex flex-wrap gap-2 mb-3">
              <Badge bg="success">Present · {counts.PRESENT}</Badge>
              <Badge bg="danger">Absent · {counts.ABSENT}</Badge>
              <Badge bg="warning" text="dark">
                Half day · {counts.HALF_DAY}
              </Badge>
              <Badge bg="info">Leave · {counts.LEAVE}</Badge>
            </div>
            <div className="d-grid gap-1" style={{ gridTemplateColumns: "repeat(7, 1fr)" }}>
              {["S", "M", "T", "W", "T", "F", "S"].map((d, i) => (
                <div key={i} className="text-center text-muted small fw-bold pb-1">
                  {d}
                </div>
              ))}
              {cells.map((day, i) => {
                if (!day) return <div key={`b${i}`} />;
                const iso = `${monthKey}-${String(day).padStart(2, "0")}`;
                const rec = byDate[iso];
                const isToday = iso === todayISO;
                const isFuture = iso > todayISO;
                return (
                  <div
                    key={iso}
                    title={
                      rec
                        ? `${rec.status}${rec.check_in ? ` · in ${rec.check_in}` : ""}${rec.check_out ? ` · out ${rec.check_out}` : ""}${rec.notes ? ` · ${rec.notes}` : ""}`
                        : isFuture
                          ? "Upcoming"
                          : "Not marked"
                    }
                    className={`text-center rounded py-1 small border ${
                      rec
                        ? `text-bg-${DAY_TONE[rec.status] || "secondary"} border-transparent`
                        : isFuture
                          ? "text-muted border-transparent"
                          : "bg-light text-muted"
                    } ${isToday ? "border-primary border-2 fw-bold" : ""}`}
                  >
                    {day}
                  </div>
                );
              })}
            </div>
            <div className="text-muted small mt-3">
              <i className="bi bi-info-circle me-1" />
              Hover any date for check in/out times and remarks. Today is
              outlined in blue.
            </div>
          </>
        )}
      </Card.Body>
    </Card>
  );
}

export default MyAttendancePanel;
