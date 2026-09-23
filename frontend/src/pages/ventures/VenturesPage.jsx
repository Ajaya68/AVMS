import { useState } from "react";
import {
  Alert,
  Badge,
  Button,
  Card,
  Col,
  Form,
  InputGroup,
  Modal,
  Row,
  Table,
} from "react-bootstrap";
import { useNavigate } from "react-router-dom";

import LoadingSpinner from "../../components/LoadingSpinner";
import { useAuth } from "../../context/AuthContext";
import { useApi } from "../../hooks/useApi";
import {
  VENTURE_BUSINESS_TYPES,
  VENTURE_STATUSES,
  createVenture,
  deleteVenture,
  fetchVentures,
  updateVenture,
} from "../../services/ventureService";

function VentureForm({ initial, onSubmit, onCancel }) {
  const [ventureName, setVentureName] = useState(initial?.venture_name ?? "");
  const [businessType, setBusinessType] = useState(
    initial?.business_type ?? "GENERAL"
  );
  const [status, setStatus] = useState(initial?.status ?? "ACTIVE");
  const [description, setDescription] = useState(initial?.description ?? "");
  const [phone, setPhone] = useState(initial?.phone ?? "");
  const [email, setEmail] = useState(initial?.email ?? "");
  const [address, setAddress] = useState(initial?.address ?? "");
  const [city, setCity] = useState(initial?.city ?? "");
  const [state, setState] = useState(initial?.state ?? "");
  const [pincode, setPincode] = useState(initial?.pincode ?? "");
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");
    setSaving(true);
    try {
      await onSubmit({
        venture_name: ventureName,
        business_type: businessType,
        status,
        description,
        phone,
        email,
        address,
        city,
        state,
        pincode,
      });
    } catch (err) {
      const errors = err?.response?.data?.errors || {};
      const first = Object.values(errors).flat()[0];
      setError(first || "Unable to save venture.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <Form onSubmit={handleSubmit}>
      {error && <Alert variant="danger" className="py-2">{error}</Alert>}
      <Row>
        <Col md={6}>
          <Form.Group className="mb-3" controlId="venture-name">
            <Form.Label>Venture name</Form.Label>
            <Form.Control
              type="text"
              value={ventureName}
              onChange={(e) => setVentureName(e.target.value)}
              required
            />
          </Form.Group>
        </Col>
        <Col md={6}>
          <Form.Group className="mb-3" controlId="venture-type">
            <Form.Label>Business type</Form.Label>
            <Form.Select
              value={businessType}
              onChange={(e) => setBusinessType(e.target.value)}
            >
              {VENTURE_BUSINESS_TYPES.map((t) => (
                <option key={t} value={t}>
                  {t.replace(/_/g, " ").toLowerCase().replace(/^\w/, (c) => c.toUpperCase())}
                </option>
              ))}
            </Form.Select>
          </Form.Group>
        </Col>
      </Row>

      <Row>
        <Col md={6}>
          <Form.Group className="mb-3" controlId="venture-phone">
            <Form.Label>Phone</Form.Label>
            <Form.Control
              type="text"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
            />
          </Form.Group>
        </Col>
        <Col md={6}>
          <Form.Group className="mb-3" controlId="venture-email">
            <Form.Label>Email</Form.Label>
            <Form.Control
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </Form.Group>
        </Col>
      </Row>

      <Row>
        <Col md={4}>
          <Form.Group className="mb-3" controlId="venture-city">
            <Form.Label>City</Form.Label>
            <Form.Control
              type="text"
              value={city}
              onChange={(e) => setCity(e.target.value)}
            />
          </Form.Group>
        </Col>
        <Col md={4}>
          <Form.Group className="mb-3" controlId="venture-state">
            <Form.Label>State</Form.Label>
            <Form.Control
              type="text"
              value={state}
              onChange={(e) => setState(e.target.value)}
            />
          </Form.Group>
        </Col>
        <Col md={4}>
          <Form.Group className="mb-3" controlId="venture-pincode">
            <Form.Label>Pincode</Form.Label>
            <Form.Control
              type="text"
              value={pincode}
              onChange={(e) => setPincode(e.target.value)}
            />
          </Form.Group>
        </Col>
      </Row>

      <Form.Group className="mb-3" controlId="venture-address">
        <Form.Label>Address</Form.Label>
        <Form.Control
          as="textarea"
          rows={2}
          value={address}
          onChange={(e) => setAddress(e.target.value)}
        />
      </Form.Group>

      <Form.Group className="mb-4" controlId="venture-description">
        <Form.Label>Description</Form.Label>
        <Form.Control
          as="textarea"
          rows={2}
          value={description}
          onChange={(e) => setDescription(e.target.value)}
        />
      </Form.Group>

      {initial && (
        <Form.Group className="mb-4" controlId="venture-status">
          <Form.Label>Status</Form.Label>
          <Form.Select
            value={status}
            onChange={(e) => setStatus(e.target.value)}
          >
            {VENTURE_STATUSES.map((s) => (
              <option key={s} value={s}>
                {s.charAt(0) + s.slice(1).toLowerCase()}
              </option>
            ))}
          </Form.Select>
        </Form.Group>
      )}

      <div className="d-flex justify-content-end gap-2">
        <Button variant="secondary" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" variant="primary" disabled={saving}>
          {saving ? "Saving..." : initial ? "Save changes" : "Create venture"}
        </Button>
      </div>
    </Form>
  );
}

function VenturesPage() {
  const navigate = useNavigate();
  const { hasPerm } = useAuth();
  const [search, setSearch] = useState("");
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState(null);

  const {
    data,
    loading,
    error,
    refetch,
  } = useApi(() => fetchVentures({ search: search || undefined }), [search]);

  const list = Array.isArray(data?.results) ? data.results : data || [];
  const canManage = hasPerm("ventures.manage");

  const toggleActive = async (venture) => {
    await updateVenture(venture.id, {
      status: venture.status === "ACTIVE" ? "INACTIVE" : "ACTIVE",
    });
    await refetch();
  };

  const handleDelete = async (venture) => {
    if (
      window.confirm(
        `Delete venture ${venture.venture_code} - ${venture.venture_name}? This cannot be undone.`
      )
    ) {
      await deleteVenture(venture.id);
      await refetch();
    }
  };

  return (
    <div>
      <div className="d-flex flex-wrap align-items-center justify-content-between mb-4">
        <div>
          <h4 className="mb-1">Ventures</h4>
          <p className="text-muted mb-0">
            Enterprise units operated within AVMS
          </p>
        </div>
        {canManage && (
          <Button
            variant="primary"
            onClick={() => {
              setEditing(null);
              setShowForm(true);
            }}
          >
            <i className="bi bi-plus-lg me-2" />
            New venture
          </Button>
        )}
      </div>

      <Card className="shadow-sm">
        <Card.Header className="bg-white">
          <div className="d-flex justify-content-between align-items-center">
            <InputGroup style={{ maxWidth: 320 }}>
              <InputGroup.Text>
                <i className="bi bi-search" />
              </InputGroup.Text>
              <Form.Control
                placeholder="Search name, code or city"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                aria-label="Search ventures"
              />
            </InputGroup>
            <span className="text-muted small">
              {data?.count ?? list.length} venture(s)
            </span>
          </div>
        </Card.Header>
        <Card.Body>
          {loading ? (
            <LoadingSpinner label="Loading ventures..." />
          ) : error ? (
            <Alert variant="danger" className="mb-0">
              Unable to load ventures. Check that the backend is running.
            </Alert>
          ) : (
            <Table hover responsive className="mb-0">
              <thead>
                <tr>
                  <th>Code</th>
                  <th>Venture</th>
                  <th>Business type</th>
                  <th>Location</th>
                  <th>Status</th>
                  <th className="text-end">Actions</th>
                </tr>
              </thead>
              <tbody>
                {list.map((venture) => (
                  <tr
                    key={venture.id}
                    style={{ cursor: "pointer" }}
                    onClick={() => navigate(`/ventures/${venture.id}`)}
                  >
                    <td className="text-muted">{venture.venture_code}</td>
                    <td>
                      <div className="fw-semibold">{venture.venture_name}</div>
                      {venture.description && (
                        <small className="text-muted d-block text-truncate" style={{ maxWidth: 260 }}>
                          {venture.description}
                        </small>
                      )}
                    </td>
                    <td>{venture.business_type.replace(/_/g, " ")}</td>
                    <td className="text-muted">
                      {[venture.city, venture.state].filter(Boolean).join(", ") || "—"}
                    </td>
                    <td>
                      <Badge bg={venture.status === "ACTIVE" ? "success" : "secondary"}>
                        {venture.status}
                      </Badge>
                    </td>
                    <td className="text-end">
                      {canManage && (
                        <>
                          <Button
                            size="sm"
                            variant="outline-secondary"
                            onClick={(e) => {
                              e.stopPropagation();
                              setEditing(venture);
                              setShowForm(true);
                            }}
                            className="me-2"
                          >
                            <i className="bi bi-pencil" />
                          </Button>
                          <Button
                            size="sm"
                            variant={venture.status === "ACTIVE" ? "outline-danger" : "outline-success"}
                            onClick={(e) => {
                              e.stopPropagation();
                              toggleActive(venture);
                            }}
                            className="me-2"
                          >
                            {venture.status === "ACTIVE" ? "Deactivate" : "Activate"}
                          </Button>
                          <Button
                            size="sm"
                            variant="outline-danger"
                            onClick={(e) => {
                              e.stopPropagation();
                              handleDelete(venture);
                            }}
                          >
                            <i className="bi bi-trash" />
                          </Button>
                        </>
                      )}
                    </td>
                  </tr>
                ))}
                {list.length === 0 && (
                  <tr>
                    <td colSpan={6} className="text-center text-muted py-4">
                      No ventures found.
                    </td>
                  </tr>
                )}
              </tbody>
            </Table>
          )}
        </Card.Body>
      </Card>

      <Modal
        show={showForm}
        onHide={() => {
          setShowForm(false);
          setEditing(null);
        }}
      >
        <Modal.Header closeButton>
          <Modal.Title>
            {editing ? "Edit venture" : "Create venture"}
          </Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <VentureForm
            key={editing?.id ?? "new"}
            initial={editing}
            onCancel={() => {
              setShowForm(false);
              setEditing(null);
            }}
            onSubmit={async (payload) => {
              if (editing) {
                await updateVenture(editing.id, payload);
              } else {
                await createVenture(payload);
              }
              setShowForm(false);
              setEditing(null);
              await refetch();
            }}
          />
        </Modal.Body>
      </Modal>
    </div>
  );
}

export default VenturesPage;