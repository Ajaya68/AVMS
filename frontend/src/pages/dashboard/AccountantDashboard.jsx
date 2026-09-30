import { Card, Col, Row } from "react-bootstrap";

import LoadingSpinner from "../../components/LoadingSpinner";
import { useAuth } from "../../context/AuthContext";
import { useApi } from "../../hooks/useApi";
import { fetchFinancialReport } from "../../services/reportService";
import MyProfileCard from "./MyProfileCard";
import { DashboardHeader, KpiCard, QuickActions } from "./widgets";
import { PERSONA_META } from "./persona";

/**
 * Accountant dashboard: money in, money out, dues and profit —
 * plus shortcuts to payments, expenses and bills.
 */
function AccountantDashboard() {
  const { hasPerm } = useAuth();
  const meta = PERSONA_META.accountant;
  const canFinance = hasPerm("payments.view") || hasPerm("expenses.view") || hasPerm("reports.view");

  const { data: financial, loading } = useApi(
    () => (canFinance ? fetchFinancialReport() : Promise.resolve(null)),
    [canFinance]
  );

  const summary = financial?.summary || {};
  const outstanding = financial?.outstanding || {};
  const cash = financial?.cash_flow || {};

  const kpis = canFinance
    ? [
        {
          key: "revenue",
          title: "Revenue (30d)",
          icon: "bi-graph-up-arrow",
          className: "bg-primary",
          value: summary.revenue ?? null,
        },
        {
          key: "expenses",
          title: "Expenses (30d)",
          icon: "bi-receipt",
          className: "bg-danger",
          value: summary.expenses ?? null,
        },
        {
          key: "profit",
          title: "Net Profit (30d)",
          icon: "bi-piggy-bank",
          className: "bg-success",
          value: summary.net_profit ?? null,
        },
        {
          key: "receivables",
          title: "Receivables",
          icon: "bi-wallet2",
          className: "bg-warning",
          value: outstanding.receivables ?? null,
        },
      ]
    : [];

  return (
    <div>
      <DashboardHeader title={meta.title} subtitle={meta.subtitle} />
      <MyProfileCard />
      <QuickActions
        actions={[
          ...(hasPerm("payments.manage") ? [{ to: "/payments", label: "Record Payment", icon: "bi-cash-coin", variant: "primary" }] : []),
          ...(hasPerm("expenses.manage") ? [{ to: "/expenses", label: "Add Expense", icon: "bi-receipt" }] : []),
          ...(hasPerm("sales.view") ? [{ to: "/sales", label: "Sales Bills", icon: "bi-cart" }] : []),
          ...(hasPerm("purchases.view") ? [{ to: "/purchases", label: "Purchase Bills", icon: "bi-bag" }] : []),
          ...(hasPerm("reports.view") ? [{ to: "/reports", label: "Reports", icon: "bi-graph-up" }] : []),
        ]}
      />

      {kpis.length > 0 && (
        <Row className="g-3 mb-4">
          {loading ? (
            <LoadingSpinner label="Loading finance..." />
          ) : (
            kpis.map((card) => <KpiCard key={card.key} {...card} />)
          )}
        </Row>
      )}

      {canFinance && !loading && financial && (
        <Row className="g-3 mb-4">
          <Col md={6}>
            <Card className="shadow-sm h-100">
              <Card.Header className="bg-white">
                <strong>Dues</strong>
              </Card.Header>
              <Card.Body className="d-flex gap-4">
                <div>
                  <div className="text-muted small">To receive</div>
                  <div className="fs-5 fw-semibold text-warning">
                    ₹{Number(outstanding.receivables || 0).toLocaleString("en-IN")}
                  </div>
                </div>
                <div>
                  <div className="text-muted small">To pay</div>
                  <div className="fs-5 fw-semibold text-danger">
                    ₹{Number(outstanding.payables || 0).toLocaleString("en-IN")}
                  </div>
                </div>
              </Card.Body>
            </Card>
          </Col>
          <Col md={6}>
            <Card className="shadow-sm h-100">
              <Card.Header className="bg-white">
                <strong>Cash Flow (30d)</strong>
              </Card.Header>
              <Card.Body className="d-flex gap-4">
                <div>
                  <div className="text-muted small">Received</div>
                  <div className="fs-5 fw-semibold text-success">
                    ₹{Number(cash.received || 0).toLocaleString("en-IN")}
                  </div>
                </div>
                <div>
                  <div className="text-muted small">Paid</div>
                  <div className="fs-5 fw-semibold">
                    ₹{Number(cash.paid || 0).toLocaleString("en-IN")}
                  </div>
                </div>
                <div>
                  <div className="text-muted small">Net</div>
                  <div className="fs-5 fw-semibold text-primary">
                    ₹{Number(cash.net || 0).toLocaleString("en-IN")}
                  </div>
                </div>
              </Card.Body>
            </Card>
          </Col>
        </Row>
      )}
    </div>
  );
}

export default AccountantDashboard;
