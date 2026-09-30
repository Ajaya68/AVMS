import { Alert, Badge, Card, Col, Row } from "react-bootstrap";

import LoadingSpinner from "../../components/LoadingSpinner";
import { useAuth } from "../../context/AuthContext";
import { useApi } from "../../hooks/useApi";
import { fetchStockMovements } from "../../services/inventoryService";
import { fetchInventoryReport } from "../../services/reportService";
import MyProfileCard from "./MyProfileCard";
import { DashboardHeader, KpiCard, QuickActions } from "./widgets";
import { PERSONA_META } from "./persona";

const OUT_TYPES = new Set(["SALE", "PURCHASE_RETURN", "ADJUSTMENT_OUT", "TRANSFER_OUT", "DAMAGE", "EXPIRED"]);

/**
 * Inventory staff dashboard: stock value, low-stock alerts, latest
 * movements and shortcuts to the stock screens.
 */
function InventoryDashboard() {
  const { hasPerm } = useAuth();
  const meta = PERSONA_META.inventory;
  const canStock = hasPerm("inventory.view") || hasPerm("reports.view");
  const canMoves = hasPerm("stock_movements.view");

  const { data: inventory, loading: invLoading } = useApi(
    () => (canStock ? fetchInventoryReport() : Promise.resolve(null)),
    [canStock]
  );
  const { data: movesData, loading: movesLoading } = useApi(
    () => (canMoves ? fetchStockMovements({ page_size: 6 }) : Promise.resolve(null)),
    [canMoves]
  );

  const summary = inventory?.summary || {};
  const lowStock = canStock ? inventory?.low_stock || [] : [];
  const moves = movesData?.results || movesData || [];
  const moveList = Array.isArray(moves) ? moves.slice(0, 6) : [];

  const kpis = canStock
    ? [
        {
          key: "stock",
          title: "Stock Value",
          icon: "bi-box-seam",
          className: "bg-info",
          value: summary.stock_value ?? null,
        },
        {
          key: "products",
          title: "Products Stocked",
          icon: "bi-boxes",
          className: "bg-primary",
          value: summary.stock_products ?? null,
          plain: true,
        },
        {
          key: "qty",
          title: "Total Quantity",
          icon: "bi-stack",
          className: "bg-success",
          value: summary.total_quantity ?? null,
          plain: true,
        },
        {
          key: "low",
          title: "Low-Stock Lines",
          icon: "bi-exclamation-triangle",
          className: "bg-warning",
          value: lowStock.length,
          plain: true,
        },
      ]
    : [];

  return (
    <div>
      <DashboardHeader title={meta.title} subtitle={meta.subtitle} />
      <MyProfileCard />
      <QuickActions
        actions={[
          ...(hasPerm("stock_movements.manage") ? [{ to: "/stock-movements", label: "Record Movement", icon: "bi-arrow-left-right", variant: "primary" }] : []),
          ...(hasPerm("products.manage") ? [{ to: "/products", label: "Products", icon: "bi-box" }] : []),
          ...(hasPerm("warehouses.manage") ? [{ to: "/warehouses", label: "Warehouses", icon: "bi-shop" }] : []),
          ...(hasPerm("inventory.view") ? [{ to: "/inventory", label: "Live Stock", icon: "bi-boxes" }] : []),
        ]}
      />

      {kpis.length > 0 && (
        <Row className="g-3 mb-4">
          {invLoading ? (
            <LoadingSpinner label="Loading stock..." />
          ) : (
            kpis.map((card) => <KpiCard key={card.key} {...card} />)
          )}
        </Row>
      )}

      <Row className="g-3 mb-4">
        {canStock && (
          <Col lg={canMoves ? 6 : 12}>
            <Card className="shadow-sm h-100">
              <Card.Header className="bg-white">
                <strong>Low Stock Alerts</strong>
              </Card.Header>
              <Card.Body>
                {invLoading ? (
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
                      {lowStock.slice(0, 6).map((row) => (
                        <tr key={row.product_code}>
                          <td>{row.product_name}</td>
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
        {canMoves && (
          <Col lg={canStock ? 6 : 12}>
            <Card className="shadow-sm h-100">
              <Card.Header className="bg-white">
                <strong>Latest Movements</strong>
              </Card.Header>
              <Card.Body>
                {movesLoading ? (
                  <LoadingSpinner label="Loading movements..." />
                ) : moveList.length === 0 ? (
                  <div className="text-muted">No movements recorded yet.</div>
                ) : (
                  <ul className="list-unstyled mb-0">
                    {moveList.map((m) => (
                      <li key={m.id} className="d-flex justify-content-between small mb-2">
                        <span>
                          <Badge bg={OUT_TYPES.has(m.movement_type) ? "warning" : "info"} text="dark" className="me-2">
                            {m.movement_type}
                          </Badge>
                          {m.product_name}
                        </span>
                        <span className="text-muted text-nowrap ms-2">
                          {m.movement_date} · {m.quantity}
                        </span>
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

export default InventoryDashboard;
