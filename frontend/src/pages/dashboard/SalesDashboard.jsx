import { Card, Col, Row } from "react-bootstrap";

import LoadingSpinner from "../../components/LoadingSpinner";
import { useAuth } from "../../context/AuthContext";
import { useApi } from "../../hooks/useApi";
import { fetchFinancialReport, fetchSalesReport } from "../../services/reportService";
import MyProfileCard from "./MyProfileCard";
import { DashboardHeader, KpiCard, QuickActions } from "./widgets";
import { PERSONA_META } from "./persona";

/**
 * Sales staff dashboard: own bills, collections due, top products
 * and shortcuts to billing screens.
 */
function SalesDashboard() {
  const { hasPerm } = useAuth();
  const meta = PERSONA_META.sales;
  const canSales = hasPerm("sales.view") || hasPerm("reports.view");
  const canDues = hasPerm("payments.view") || hasPerm("reports.view");

  const { data: sales, loading: salesLoading } = useApi(
    () => (canSales ? fetchSalesReport() : Promise.resolve(null)),
    [canSales]
  );
  const { data: financial, loading: duesLoading } = useApi(
    () => (canDues ? fetchFinancialReport() : Promise.resolve(null)),
    [canDues]
  );

  const summary = sales?.summary || {};
  const topProducts = sales?.top_products || [];

  const kpis = [
    canSales && {
      key: "sales",
      title: "Net Sales (30d)",
      icon: "bi-cash-stack",
      className: "bg-primary",
      value: summary.net_amount ?? null,
    },
    canSales && {
      key: "bills",
      title: "Bills (30d)",
      icon: "bi-receipt",
      className: "bg-info",
      value: summary.count ?? null,
      plain: true,
    },
    canSales && {
      key: "items",
      title: "Items Sold (30d)",
      icon: "bi-box-seam",
      className: "bg-success",
      value: summary.items_sold ?? null,
      plain: true,
    },
    canDues && {
      key: "receivables",
      title: "To Collect",
      icon: "bi-wallet2",
      className: "bg-warning",
      value: financial?.outstanding?.receivables ?? null,
    },
  ].filter(Boolean);

  const loading = (canSales && salesLoading) || (canDues && duesLoading);

  return (
    <div>
      <DashboardHeader title={meta.title} subtitle={meta.subtitle} />
      <MyProfileCard />
      <QuickActions
        actions={[
          ...(hasPerm("sales.manage") ? [{ to: "/sales", label: "New Sale", icon: "bi-cart-plus", variant: "primary" }] : []),
          ...(hasPerm("customers.manage") ? [{ to: "/customers", label: "Customers", icon: "bi-people" }] : []),
          ...(hasPerm("sales_returns.manage") ? [{ to: "/sales-returns", label: "Sale Returns", icon: "bi-arrow-counterclockwise" }] : []),
          ...(hasPerm("payments.view") ? [{ to: "/payments", label: "Collections", icon: "bi-cash-coin" }] : []),
        ]}
      />

      {kpis.length > 0 && (
        <Row className="g-3 mb-4">
          {loading ? (
            <LoadingSpinner label="Loading sales..." />
          ) : (
            kpis.map((card) => <KpiCard key={card.key} {...card} />)
          )}
        </Row>
      )}

      {canSales && !salesLoading && topProducts.length > 0 && (
        <Row className="g-3 mb-4">
          <Col>
            <Card className="shadow-sm">
              <Card.Header className="bg-white">
                <strong>Top Products (30d)</strong>
              </Card.Header>
              <Card.Body className="p-0">
                <table className="table mb-0">
                  <thead>
                    <tr>
                      <th className="ps-3">Product</th>
                      <th className="text-end">Qty</th>
                      <th className="text-end pe-3">Revenue</th>
                    </tr>
                  </thead>
                  <tbody>
                    {topProducts.slice(0, 5).map((p) => (
                      <tr key={p.product}>
                        <td className="ps-3">
                          {p.product_name}{" "}
                          <small className="text-muted">{p.product_code}</small>
                        </td>
                        <td className="text-end">{p.quantity}</td>
                        <td className="text-end pe-3">
                          ₹{Number(p.revenue || 0).toLocaleString("en-IN")}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </Card.Body>
            </Card>
          </Col>
        </Row>
      )}

      {topProducts.length === 0 && !loading && (
        <p className="text-muted small">
          No sales in the last 30 days. Create your first bill from Sales.
        </p>
      )}
    </div>
  );
}

export default SalesDashboard;
