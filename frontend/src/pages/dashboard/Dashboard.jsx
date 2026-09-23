import { Alert, Card, Col, Row, Badge } from "react-bootstrap";
import LoadingSpinner from "../../components/LoadingSpinner";
import { useApi } from "../../hooks/useApi";
import { getHealth } from "../../services/systemService";

const KPI_CARDS = [
  { title: "Total Sales", icon: "bi-cash-stack", className: "bg-primary" },
  { title: "Total Purchases", icon: "bi-cart-plus", className: "bg-success" },
  { title: "Total Expenses", icon: "bi-wallet2", className: "bg-danger" },
  { title: "Inventory Value", icon: "bi-box-seam", className: "bg-warning" },
];

function Dashboard() {
  const { data: health, loading, error } = useApi(() => getHealth(), []);

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
          <Col key={card.title} xs={12} sm={6} xl={3}>
            <Card className="shadow-sm h-100">
              <Card.Body className="d-flex align-items-center gap-3">
                <div className={`kpi-icon ${card.className}`}>
                  <i className={`bi ${card.icon}`} />
                </div>
                <div>
                  <div className="text-muted small">{card.title}</div>
                  <div className="fs-4 fw-semibold">--</div>
                </div>
              </Card.Body>
            </Card>
          </Col>
        ))}
      </Row>

      <Card className="shadow-sm">
        <Card.Header className="bg-white">
          <strong>Backend Status</strong>
        </Card.Header>
        <Card.Body>
          {loading ? (
            <LoadingSpinner label="Contacting backend..." />
          ) : error ? (
            <Alert variant="danger" className="mb-0">
              Backend unreachable. Ensure the Django server is running on
              port 8000.
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
                KPI values and charts are populated as modules are completed
                in later phases.
              </Col>
            </Row>
          )}
        </Card.Body>
      </Card>
    </div>
  );
}

export default Dashboard;