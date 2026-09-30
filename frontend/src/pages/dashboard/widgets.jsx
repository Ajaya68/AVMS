import { Card, Col } from "react-bootstrap";
import { Link } from "react-router-dom";

export function KpiCard({ icon, className, title, value, plain }) {
  return (
    <Col xs={12} sm={6} xl={3}>
      <Card className="shadow-sm h-100">
        <Card.Body className="d-flex align-items-center gap-3">
          <div className={`kpi-icon ${className}`}>
            <i className={`bi ${icon}`} />
          </div>
          <div className="overflow-hidden">
            <div className="text-muted small">{title}</div>
            <div className="fs-4 fw-semibold text-truncate">
              {value === null || value === undefined
                ? "—"
                : plain
                  ? value
                  : money2(value)}
            </div>
          </div>
        </Card.Body>
      </Card>
    </Col>
  );
}

export function QuickActions({ actions }) {
  if (!actions?.length) return null;
  return (
    <Card className="shadow-sm mb-4">
      <Card.Body className="d-flex flex-wrap gap-2">
        {actions.map((a) => (
          <Link
            key={a.to + a.label}
            to={a.to}
            className={`btn btn-${a.variant || "outline-primary"} btn-sm`}
          >
            <i className={`bi ${a.icon} me-2`} />
            {a.label}
          </Link>
        ))}
      </Card.Body>
    </Card>
  );
}

export function DashboardHeader({ title, subtitle }) {
  return (
    <div className="d-flex flex-wrap align-items-center justify-content-between mb-4">
      <div>
        <h4 className="mb-1">{title}</h4>
        <p className="text-muted mb-0">{subtitle}</p>
      </div>
    </div>
  );
}
