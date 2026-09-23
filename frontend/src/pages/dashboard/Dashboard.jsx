import { Alert, Badge, Card, Col, Row } from "react-bootstrap";
import { Link } from "react-router-dom";

import LoadingSpinner from "../../components/LoadingSpinner";
import { useApi } from "../../hooks/useApi";
import { getHealth } from "../../services/systemService";
import { fetchAuditLogs } from "../../services/auditLogService";
import {
  fetchFinancialReport,
  fetchInventoryReport,
  fetchPurchasesReport,
  fetchSalesReport,
} from "../../services/reportService";

const KPI_CARDS = [
  { key: "sales", title: "Net Sales (30d)", icon: "bi-cash-stack", className: "bg-primary" },
  { key: "purchases", title: "Net Purchases (30d)", icon: "bi-cart-plus", className: "bg-success" },
  { key: "receivables", title: "Receivables", icon: "bi-wallet2", className: "bg-warning" },
  { key: "stock", title: "Stock Value", icon: "bi-box-seam", className: "bg-info" },
];

const money2 = (value) =>
  new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
  }).format(Number(value || 0));

function formatTime(value) {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "—";
  return date.toLocaleString(undefined, {
    day: "numeric",
    month: "short",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function Dashboard() {
  const { data: health, loading: healthLoading, error: healthError } = useApi(
    () => getHealth(),
    []
  );

  const { data: sales, loading: salesLoading, error: salesError } = useApi(
    () => fetchSalesReport(),
    []
  );
  const { data: purchases, loading: purchasesLoading, error: purchasesError } = useApi(
    () => fetchPurchasesReport(),
    []
  );
  const { data: financial, loading: financialLoading, error: financialError } = useApi(
    () => fetchFinancialReport(),
    []
  );
  const { data: inventory, loading: inventoryLoading, error: inventoryError } = useApi(
    () => fetchInventoryReport(),
    []
  );
  const { data: audit, loading: auditLoading, error: auditError } = useApi(
    () => fetchAuditLogs({ page_size: 5 }),
    []
  );

  const loading =
    salesLoading || purchasesLoading || financialLoading || inventoryLoading;
  const error =
    salesError || purchasesError || financialError || inventoryError;

  const values = {
    sales: sales?.summary?.net_amount ?? null,
    purchases: purchases?.summary?.net_amount ?? null,
    receivables: financial?.outstanding?.receivables ?? null,
    stock: inventory?.summary?.stock_value ?? null,
  };

  const activity = audit?.results?.slice(0, 5) || [];
  const lowStock = inventory?.low_stock || [];

  return (
    <div>
      <div className="d-flex flex-wrap align-items-center justify-content-between mb-4">
        <div>
          <h4 className="mb-1">Dashboard</h4>
          <p className="text-muted mb-0">
            Ajaya Venture Management System - central overview
          </p>
        </div>
      </div>

      <Row className="g-3 mb-4">
        {KPI_CARDS.map((card) => (
          <Col key={card.key} xs={12} sm={6} xl={3}>
            <Card className="shadow-sm h-100">
              <Card.Body className="d-flex align-items-center gap-3">
                <div className={`kpi-icon ${card.className}`}>
                  <i className={`bi ${card.icon}`} />
                </div>
                <div className="overflow-hidden">
                  <div className="text-muted small">{card.title}</div>
                  <div className="fs-4 fw-semibold text-truncate">
                    {values[card.key] === null
                      ? "—"
                      : money2(values[card.key])}
                  </div>
                </div>
              </Card.Body>
            </Card>
          </Col>
        ))}
      </Row>

      {loading ? (
        <LoadingSpinner label="Loading overview..." />
      ) : error ? (
        <Alert variant="danger">Unable to load dashboard data.</Alert>
      ) : (
        <Row className="g-3 mb-4">
          <Col lg={6}>
            <Card className="shadow-sm h-100">
              <Card.Header className="bg-white d-flex justify-content-between align-items-center">
                <strong>Low Stock Alerts</strong>
                <Link to="/inventory" className="small text-decoration-none">
                  Inventory
                </Link>
              </Card.Header>
              <Card.Body>
                {lowStock.length === 0 ? (
                  <Alert variant="success" className="mb-0">
                    <i className="bi bi-check-circle me-2" />
                    Everything is comfortably stocked.
                  </Alert>
                ) : (
                  <table className="table table-sm mb-0">
                    <thead>
                      <tr>
                        <th>Product</th>
                        <th className="text-end">On Hand</th>
                        <th className="text-end">Reorder</th>
                      </tr>
                    </thead>
                    <tbody>
                      {lowStock.map((row) => (
                        <tr key={row.product_code}>
                          <td>
                            <div>{row.product_name}</div>
                            <small className="text-muted">
                              {row.product_code}
                            </small>
                          </td>
                          <td className="text-end">
                            <Badge bg="danger">{row.quantity}</Badge>
                          </td>
                          <td className="text-end text-muted">
                            {row.reorder_level}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
              </Card.Body>
            </Card>
          </Col>

          <Col lg={6}>
            <Card className="shadow-sm h-100">
              <Card.Header
                className={`bg-white d-flex justify-content-between align-items-center ${
                  auditLoading ? "opacity-50" : ""
                }`}
              >
                <strong>Recent Activity</strong>
                <Link to="/audit-logs" className="small text-decoration-none">
                  View all
                </Link>
              </Card.Header>
              <Card.Body>
                {auditError ? (
                  <Alert variant="danger" className="mb-0">
                    Unable to load recent activity.
                  </Alert>
                ) : activity.length === 0 ? (
                  <div className="text-center text-muted py-4">
                    No activity recorded yet.
                  </div>
                ) : (
                  <ul className="timeline mb-0">
                    {activity.map((entry) => (
                      <li key={entry.id} className="timeline-item">
                        <div className="d-flex justify-content-between align-items-center">
                          <div className="fw-semibold small">
                            {entry.description}
                          </div>
                          <small className="text-muted text-nowrap ms-3">
                            {formatTime(entry.created_at)}
                          </small>
                        </div>
                        <div>
                          <Badge bg="light" text="dark" className="me-1">
                            {entry.module}
                          </Badge>
                          <Badge bg="light" text="dark" className="me-1">
                            {entry.action}
                          </Badge>
                          <small className="text-muted">
                            {entry.user || "system"}
                          </small>
                        </div>
                      </li>
                    ))}
                  </ul>
                )}
              </Card.Body>
            </Card>
          </Col>
        </Row>
      )}

      <Card className="shadow-sm">
        <Card.Header className="bg-white">
          <strong>Backend Status</strong>
        </Card.Header>
        <Card.Body>
          {healthLoading ? (
            <LoadingSpinner label="Contacting backend..." />
          ) : healthError ? (
            <Alert variant="danger" className="mb-0">
              Backend unreachable. Ensure the Django server is running on port
              8000.
            </Alert>
          ) : (
            <Row className="align-items-center">
              <Col>
                <div className="d-flex align-items-center gap-2 mb-1">
                  <Badge bg={health.status === "ok" ? "success" : "warning"}>
                    {health.status}
                  </Badge>
                  <strong>{health.service}</strong>
                </div>
                <div className="text-muted">
                  <span>Version {health.version}</span>
                  {" · "}Database:{" "}
                  <Badge bg={health.database === "ok" ? "success" : "danger"} pill>
                    {health.database}
                  </Badge>
                </div>
              </Col>
              <Col className="text-muted small text-md-end">
                <i className="bi bi-info-circle me-1" />
                Sales, purchases and stock figures cover the last 30 days.
              </Col>
            </Row>
          )}
        </Card.Body>
      </Card>
    </div>
  );
}

export default Dashboard;