import { Alert, Badge, Card, Col, Row } from "react-bootstrap";

import LoadingSpinner from "../../components/LoadingSpinner";
import { useApi } from "../../hooks/useApi";
import { fetchRoles } from "../../services/userService";

function RolesPage() {
  const { data, loading, error } = useApi(() => fetchRoles(), []);
  const roles = data || [];

  return (
    <div>
      <div className="d-flex flex-wrap align-items-center justify-content-between mb-4">
        <div>
          <h4 className="mb-1">Roles</h4>
          <p className="text-muted mb-0">
            Capability groups assigned to users
          </p>
        </div>
      </div>

      {loading ? (
        <LoadingSpinner label="Loading roles..." />
      ) : error ? (
        <Alert variant="danger">Unable to load roles.</Alert>
      ) : (
        <Row className="g-3">
          {roles.map((role) => (
            <Col key={role.id} xs={12} lg={6} xl={4}>
              <Card className="shadow-sm h-100">
                <Card.Header className="bg-white d-flex align-items-center justify-content-between">
                  <strong>{role.name}</strong>
                  <Badge bg="secondary">{role.code}</Badge>
                </Card.Header>
                <Card.Body>
                  <p className="text-muted small mb-3">
                    {role.description || "No description provided."}
                  </p>
                  <div className="mb-1 small text-uppercase text-muted">
                    Granted permissions
                  </div>
                  <div className="d-flex flex-wrap gap-1">
                    {role.permission_codes?.length ? (
                      <>
                        {role.permission_codes
                          .filter((code) => code.endsWith(".view"))
                          .map((code) => (
                            <Badge key={code} bg="light" text="primary">
                              {code}
                            </Badge>
                          ))}
                        {role.permission_codes
                          .filter((code) => code.endsWith(".manage"))
                          .map((code) => (
                            <Badge key={code} bg="light" text="success">
                              {code}
                            </Badge>
                          ))}
                      </>
                    ) : (
                      <span className="text-muted small">No permissions</span>
                    )}
                  </div>
                </Card.Body>
              </Card>
            </Col>
          ))}
        </Row>
      )}
    </div>
  );
}

export default RolesPage;