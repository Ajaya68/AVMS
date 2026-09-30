import { Alert, Badge, Card, Col, Row } from "react-bootstrap";
import { Link } from "react-router-dom";

import LoadingSpinner from "../../components/LoadingSpinner";
import { useAuth } from "../../context/AuthContext";
import { useApi } from "../../hooks/useApi";
import { getHealth } from "../../services/systemService";
import { fetchAuditLogs } from "../../services/auditLogService";
import {
  fetchFinancialReport,
  fetchInventoryReport,
  fetchPurchasesReport,
  fetchSalesReport,
} from "../../services/reportService";
import MyProfileCard from "./MyProfileCard";
import { DashboardHeader, KpiCard, QuickActions } from "./widgets";
import { PERSONA_META } from "./persona";

/**
 * Admin dashboard: every KPI, quick links to admin-only modules
 * (users, roles, audit), low-stock alerts, recent activity and
 * backend status.
 */
function AdminDashboard() {
  const { hasPerm } = useAuth();
  const meta = PERSONA_META.admin;
  // Admins hold every permission; keep the same perm gates so custom
  // setups degrade gracefully instead of flashing 403s.
  const canSales = hasPerm("sales.view") || hasPerm("reports.view");
  const canPurchases = hasPerm("purchases.view") || hasPerm("reports.view");
  const canFinance =
    hasPerm("payments.view") ||
    hasPerm("expenses.view") ||
    hasPerm("reports.view");
  const canStock = hasPerm("inventory.view") || hasPerm("reports.view");
  const canAudit = hasPerm("audit.view");

  const { data: health, loading: healthLoading, error: healthError } = useApi(
    () => getHealth(),
    []
  );
  const { data: sales, loading: salesLoading, error: salesError } = useApi(
    () => (canSales ? fetchSalesReport() : Promise.resolve(null)),
    [canSales]
  );
  const { data: purchases, loading: purchasesLoading, error: purchasesError } = useApi(
    () => (canPurchases ? fetchPurchasesReport() : Promise.resolve(null)),
    [canPurchases]
  );
  const { data: financial, loading: financialLoading, error: financialError } = useApi(
    () => (canFinance ? fetchFinancialReport() : Promise.resolve(null)),
    [canFinance]
  );
  const { data: inventory, loading: inventoryLoading, error: inventoryError } = useApi(
    () => (canStock ? fetchInventoryReport() : Promise.resolve(null)),
    [canStock]
  );
  const { data: audit, loading: auditLoading, error: auditError } = useApi(
    () => (canAudit ? fetchAuditLogs({ page_size: 5 }) : Promise.resolve(null)),
    [canAudit]
  );

  const kpis = [
    canSales && {
      key: "sales",
      title: "Net Sales (30d)",
      icon: "bi-cash-stack",
      className: "bg-primary",
      value: sales?.summary?.net_amount ?? null,
    },
    canPurchases && {
      key: "purchases",
      title: "Net Purchases (30d)",
      icon: "bi-cart-plus",
      className: "bg-success",
      value: purchases?.summary?.net_amount ?? null,
    },
    canFinance && {
      key: "receivables",
      title: "Receivables",
      icon: "bi-wallet2",
      className: "bg-warning",
      value: financial?.outstanding?.receivables ?? null,
    },
    canStock && {
      key: "stock",
      title: "Stock Value",
      icon: "bi-box-seam",
      className: "bg-info",
      value: inventory?.summary?.stock_value ?? null,
    },
  ].filter(Boolean);

  const kpiLoading =
    (canSales && salesLoading) ||
    (canPurchases && purchasesLoading) ||
    (canFinance && financialLoading) ||
    (canStock && inventoryLoading);
  const kpiError =
    (canSales && salesError) ||
    (canPurchases && purchasesError) ||
    (canFinance && financialError) ||
    (canStock && inventoryError);

  const activity = audit?.results?.slice(0, 5) || [];
  const lowStock = canStock ? inventory?.low_stock || [] : [];

  const formatTime = (value) => {
    if (!value) return "";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return "—";
    return date.toLocaleString(undefined, {
      day: "numeric",
      month: "short",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  return (
    <div>
      <DashboardHeader title={meta.title} subtitle={meta.subtitle} />
      <MyProfileCard />
      <QuickActions
        actions={[
          ...(hasPerm("users.view") ? [{ to: "/users", label: "Users", icon: "bi-people" }] : []),
          ...(hasPerm("roles.view") ? [{ to: "/roles", label: "Roles", icon: "bi-shield-check" }] : []),
          ...(hasPerm("reports.view") ? [{ to: "/reports", label: "Reports", icon: "bi-graph-up" }] : []),
          ...(hasPerm("audit.view") ? [{ to: "/audit-logs", label: "Audit Logs", icon: "bi-journal-text" }] : []),
          ...(hasPerm("attendance.view") ? [{ to: "/attendance", label: "Attendance", icon: "bi-calendar-check" }] : []),
        ]}
      />

      {kpis.length > 0 && (
        <Row className="g-3 mb-4">
          {kpiLoading ? (
            <LoadingSpinner label="Loading overview..." />
          ) : kpiError ? (
            <Alert variant="danger">Unable to load dashboard data.</Alert>
          ) : (
            kpis.map((card) => <KpiCard key={card.key} {...card} />)
          )}
        </Row>
      )}

      {(canStock || canAudit) && (
        <Row className="g-3 mb-4">
          {canStock && (
            <Col lg={canAudit ? 6 : 12}>
              <Card className="shadow-sm h-100">
                <Card.Header className="bg-white d-flex justify-content-between align-items-center">
                  <strong>Low Stock Alerts</strong>
                  <Link to="/inventory" className="small text-decoration-none">
                    Inventory
                  </Link>
                </Card.Header>
                <Card.Body>
                  {inventoryLoading ? (
                    <LoadingSpinner label="Checking stock levels..." />
                  ) : lowStock.length === 0 ? (
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
                              <small className="text-muted">{row.product_code}</small>
                            </td>
                            <td className="text-end">
                              <Badge bg="danger">{row.quantity}</Badge>
                            </td>
                            <td className="text-end text-muted">{row.reorder_level}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  )}
                </Card.Body>
              </Card>
            </Col>
          )}

          {canAudit && (
            <Col lg={canStock ? 6 : 12}>
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
                            <div className="fw-semibold small">{entry.description}</div>
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
                              {entry.user_email || "system"}
                            </small>
                          </div>
                        </li>
                      ))}
                    </ul>
                  )}
                </Card.Body>
              </Card>
            </Col>
          )}
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
              Backend unreachable. Ensure the backend server is running.
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

export default AdminDashboard;
