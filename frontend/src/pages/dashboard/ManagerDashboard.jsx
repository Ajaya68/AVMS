import { Alert, Badge, Card, Col, Row } from "react-bootstrap";
import { Link } from "react-router-dom";

import LoadingSpinner from "../../components/LoadingSpinner";
import { useAuth } from "../../context/AuthContext";
import { useApi } from "../../hooks/useApi";
import {
  fetchFinancialReport,
  fetchInventoryReport,
  fetchPurchasesReport,
  fetchSalesReport,
} from "../../services/reportService";
import { fetchAttendance } from "../../services/employeeService";
import MyProfileCard from "./MyProfileCard";
import { DashboardHeader, KpiCard, QuickActions } from "./widgets";
import { PERSONA_META } from "./persona";

const todayISO = () => new Date().toISOString().slice(0, 10);

/**
 * Manager dashboard: day-to-day operations — sales, purchases, dues,
 * stock, who is in today, and shortcuts to the working screens.
 */
function ManagerDashboard() {
  const { hasPerm } = useAuth();
  const meta = PERSONA_META.manager;
  const canSales = hasPerm("sales.view") || hasPerm("reports.view");
  const canPurchases = hasPerm("purchases.view") || hasPerm("reports.view");
  const canFinance = hasPerm("payments.view") || hasPerm("reports.view");
  const canStock = hasPerm("inventory.view") || hasPerm("reports.view");
  const canTeam = hasPerm("attendance.view");

  const { data: sales, loading: salesLoading } = useApi(
    () => (canSales ? fetchSalesReport() : Promise.resolve(null)),
    [canSales]
  );
  const { data: purchases, loading: purchasesLoading } = useApi(
    () => (canPurchases ? fetchPurchasesReport() : Promise.resolve(null)),
    [canPurchases]
  );
  const { data: financial, loading: financialLoading } = useApi(
    () => (canFinance ? fetchFinancialReport() : Promise.resolve(null)),
    [canFinance]
  );
  const { data: inventory, loading: inventoryLoading } = useApi(
    () => (canStock ? fetchInventoryReport() : Promise.resolve(null)),
    [canStock]
  );
  const { data: teamData, loading: teamLoading } = useApi(() => {
    if (!canTeam) return Promise.resolve(null);
    const t = todayISO();
    return fetchAttendance({ from: t, to: t, page_size: 200 });
  }, [canTeam]);

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

  const lowStock = canStock ? inventory?.low_stock || [] : [];
  const team = teamData?.results || [];
  const present = team.filter((r) => r.status === "PRESENT").length;
  const absent = team.filter((r) => r.status === "ABSENT").length;
  const onLeave = team.filter((r) => r.status === "LEAVE").length;

  return (
    <div>
      <DashboardHeader title={meta.title} subtitle={meta.subtitle} />
      <MyProfileCard />
      <QuickActions
        actions={[
          ...(hasPerm("sales.manage") ? [{ to: "/sales", label: "New Sale", icon: "bi-cart-plus", variant: "primary" }] : []),
          ...(hasPerm("purchases.manage") ? [{ to: "/purchases", label: "New Purchase", icon: "bi-bag-plus" }] : []),
          ...(hasPerm("stock_movements.manage") ? [{ to: "/stock-movements", label: "Record Movement", icon: "bi-arrow-left-right" }] : []),
          ...(hasPerm("reports.view") ? [{ to: "/reports", label: "Reports", icon: "bi-graph-up" }] : []),
          ...(hasPerm("attendance.view") ? [{ to: "/attendance", label: "Team Attendance", icon: "bi-calendar-check" }] : []),
        ]}
      />

      {kpis.length > 0 && (
        <Row className="g-3 mb-4">
          {kpiLoading ? (
            <LoadingSpinner label="Loading overview..." />
          ) : (
            kpis.map((card) => <KpiCard key={card.key} {...card} />)
          )}
        </Row>
      )}

      <Row className="g-3 mb-4">
        {canTeam && (
          <Col lg={canStock ? 6 : 12}>
            <Card className="shadow-sm h-100">
              <Card.Header className="bg-white d-flex justify-content-between align-items-center">
                <strong>Team Today</strong>
                <Link to="/attendance" className="small text-decoration-none">
                  Attendance
                </Link>
              </Card.Header>
              <Card.Body>
                {teamLoading ? (
                  <LoadingSpinner label="Checking roster..." />
                ) : team.length === 0 ? (
                  <div className="text-muted">Nobody marked yet today.</div>
                ) : (
                  <div className="d-flex gap-2 flex-wrap">
                    <Badge bg="success">Present · {present}</Badge>
                    <Badge bg="danger">Absent · {absent}</Badge>
                    <Badge bg="info">Leave · {onLeave}</Badge>
                    <span className="text-muted small ms-auto">
                      {team.length} marked
                    </span>
                  </div>
                )}
              </Card.Body>
            </Card>
          </Col>
        )}
        {canStock && (
          <Col lg={canTeam ? 6 : 12}>
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
                  <ul className="mb-0 ps-3">
                    {lowStock.slice(0, 5).map((row) => (
                      <li key={row.product_code} className="small mb-1">
                        {row.product_name}{" "}
                        <Badge bg="danger">{row.quantity}</Badge>
                      </li>
                    ))}
                  </ul>
                )}
              </Card.Body>
            </Card>
          </Col>
        )}
      </Row>
    </div>
  );
}

export default ManagerDashboard;
