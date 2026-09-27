import { useState } from "react";
import { Alert, Badge, Button, Card, Col, Form, InputGroup, Row } from "react-bootstrap";

import { useAuth } from "../../context/AuthContext";
import { changePassword } from "../../services/authService";

function PasswordField({ label, value, onChange, show, onToggleShow }) {
  return (
    <Form.Group className="mb-3">
      <Form.Label>{label}</Form.Label>
      <InputGroup>
        <Form.Control
          type={show ? "text" : "password"}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          required
          minLength={8}
          autoComplete="new-password"
        />
        <Button variant="outline-secondary" onClick={onToggleShow} title={show ? "Hide" : "Show"}>
          <i className={`bi ${show ? "bi-eye-slash" : "bi-eye"}`} />
        </Button>
      </InputGroup>
    </Form.Group>
  );
}

export default function SettingsPage() {
  const { user } = useAuth();
  const [current, setCurrent] = useState("");
  const [next, setNext] = useState("");
  const [confirm, setConfirm] = useState("");
  const [show, setShow] = useState({ current: false, next: false, confirm: false });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [done, setDone] = useState("");

  const toggle = (key) => setShow((s) => ({ ...s, [key]: !s[key] }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setDone("");
    if (next !== confirm) {
      setError("New passwords do not match.");
      return;
    }
    setSaving(true);
    try {
      await changePassword(current, next);
      setDone("Password changed successfully.");
      setCurrent("");
      setNext("");
      setConfirm("");
    } catch (err) {
      const errors = err?.response?.data?.errors || {};
      const first = Object.values(errors).flat()[0];
      setError(first || err?.response?.data?.message || "Unable to change password.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div>
      <h4 className="mb-1">Settings</h4>
      <p className="text-muted mb-4">Your profile and account security</p>
      <Row className="g-3">
        <Col lg={6}>
          <Card className="shadow-sm h-100">
            <Card.Header className="bg-white">
              <strong>Profile</strong>
            </Card.Header>
            <Card.Body>
              <Row className="mb-2">
                <Col xs={4} className="text-muted">Email</Col>
                <Col className="fw-semibold">{user?.email || "-"}</Col>
              </Row>
              <Row className="mb-2">
                <Col xs={4} className="text-muted">Full name</Col>
                <Col>{user?.full_name || "-"}</Col>
              </Row>
              <Row className="mb-2">
                <Col xs={4} className="text-muted">Phone</Col>
                <Col>{user?.phone || "-"}</Col>
              </Row>
              <Row className="mb-2">
                <Col xs={4} className="text-muted">Roles</Col>
                <Col>
                  {(user?.role_codes || []).length > 0 ? (
                    user.role_codes.map((r) => (
                      <Badge bg="primary" className="me-1" key={r}>{r}</Badge>
                    ))
                  ) : (
                    <span className="text-muted">-</span>
                  )}
                  {user?.is_superuser && (
                    <Badge bg="danger" className="ms-1">SUPERUSER</Badge>
                  )}
                </Col>
              </Row>
              <Row>
                <Col xs={4} className="text-muted">Capabilities</Col>
                <Col>{(user?.permissions || []).length} granted</Col>
              </Row>
            </Card.Body>
          </Card>
        </Col>
        <Col lg={6}>
          <Card className="shadow-sm h-100">
            <Card.Header className="bg-white">
              <strong>Change password</strong>
            </Card.Header>
            <Card.Body>
              {error && <Alert variant="danger" className="py-2">{error}</Alert>}
              {done && <Alert variant="success" className="py-2">{done}</Alert>}
              <Form onSubmit={handleSubmit}>
                <PasswordField label="Current password" value={current} onChange={setCurrent} show={show.current} onToggleShow={() => toggle("current")} />
                <PasswordField label="New password (min 8 characters)" value={next} onChange={setNext} show={show.next} onToggleShow={() => toggle("next")} />
                <PasswordField label="Confirm new password" value={confirm} onChange={setConfirm} show={show.confirm} onToggleShow={() => toggle("confirm")} />
                <Button type="submit" variant="primary" disabled={saving}>
                  {saving ? "Saving..." : "Change password"}
                </Button>
              </Form>
            </Card.Body>
          </Card>
        </Col>
      </Row>
    </div>
  );
}
