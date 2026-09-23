import { Alert, Badge, Card, Col, Row, Button } from "react-bootstrap";
import { useNavigate, useParams } from "react-router-dom";

import LoadingSpinner from "../../components/LoadingSpinner";
import { useApi } from "../../hooks/useApi";
import { fetchVenture } from "../../services/ventureService";

function Field({ label, value }) {
  return (
    <div className="mb-3">
      <div className="text-muted small">{label}</div>
      <div>{value || "—"}</div>
    </div>
  );
}

function VentureDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();

  const { data: venture, loading, error } = useApi(() => fetchVenture(id), [id]);

  return (
    <div>
      <div className="d-flex align-items-center gap-3 mb-4">
        <Button variant="outline-secondary" size="sm" onClick={() => navigate("/ventures")}>
          <i className="bi bi-arrow-left me-2" />
          Back
        </Button>
        <div>
          <h4 className="mb-1">
            {venture ? `${venture.venture_code} - ${venture.venture_name}` : "Venture"}
          </h4>
          {venture && (
            <Badge bg={venture.status === "ACTIVE" ? "success" : "secondary"}>
              {venture.status}
            </Badge>
          )}
        </div>
      </div>

      {loading ? (
        <LoadingSpinner label="Loading venture..." />
      ) : error ? (
        <Alert variant="danger">Unable to load venture.</Alert>
      ) : venture ? (
        <Card className="shadow-sm">
          <Card.Body>
            <Row>
              <Col md={6}>
                <Field label="Venture code" value={venture.venture_code} />
                <Field label="Business type" value={venture.business_type.replace(/_/g, " ")} />
                <Field label="Phone" value={venture.phone} />
                <Field label="Email" value={venture.email} />
              </Col>
              <Col md={6}>
                <Field label="Address" value={venture.address} />
                <Field label="City" value={venture.city} />
                <Field label="State" value={venture.state} />
                <Field label="Pincode" value={venture.pincode} />
              </Col>
            </Row>
            {venture.description && (
              <Field label="Description" value={venture.description} />
            )}
          </Card.Body>
        </Card>
      ) : (
        <Alert variant="warning">Venture not found.</Alert>
      )}
    </div>
  );
}

export default VentureDetailPage;