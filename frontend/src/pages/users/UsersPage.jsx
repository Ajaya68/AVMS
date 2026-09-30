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
} from "react-bootstrap";

import LoadingSpinner from "../../components/LoadingSpinner";
import { useApi } from "../../hooks/useApi";
import { useAuth } from "../../context/AuthContext";
import {
  createUser,
  deleteUser,
  fetchRoles,
  fetchUsers,
  updateUser,
} from "../../services/userService";

function UserForm({ initial, roles, onSubmit, onCancel }) {
  const [email, setEmail] = useState(initial?.email ?? "");
  const [password, setPassword] = useState("");
  const [fullName, setFullName] = useState(initial?.full_name ?? "");
  const [phone, setPhone] = useState(initial?.phone ?? "");
  const [roleCodes, setRoleCodes] = useState(initial?.role_codes ?? []);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");
    setSaving(true);
    try {
      await onSubmit({
        email,
        full_name: fullName,
        phone,
        roles: roleCodes,
        ...(initial ? {} : { password }),
      });
    } catch (err) {
      const errors = err?.response?.data?.errors || {};
      const first = Object.values(errors).flat()[0];
      setError(first || "Unable to save user.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <Form onSubmit={handleSubmit}>
      {error && <Alert variant="danger" className="py-2">{error}</Alert>}
      <Row>
        <Col md={6}>
          <Form.Group className="mb-3" controlId="user-email">
            <Form.Label>Email</Form.Label>
            <Form.Control
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              disabled={Boolean(initial)}
            />
          </Form.Group>
        </Col>
        <Col md={6}>
          <Form.Group className="mb-3" controlId="user-full-name">
            <Form.Label>Full name</Form.Label>
            <Form.Control
              type="text"
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
            />
          </Form.Group>
        </Col>
      </Row>
      <Row>
        <Col md={6}>
          <Form.Group className="mb-3" controlId="user-phone">
            <Form.Label>Phone</Form.Label>
            <Form.Control
              type="text"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
            />
          </Form.Group>
        </Col>
        {!initial && (
          <Col md={6}>
            <Form.Group className="mb-3" controlId="user-password">
              <Form.Label>Password</Form.Label>
              <Form.Control
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                minLength={8}
              />
            </Form.Group>
          </Col>
        )}
      </Row>
      <Form.Group className="mb-4" controlId="user-roles">
        <Form.Label>Roles</Form.Label>
        <div>
          {roles.map((role) => (
            <Form.Check
              key={role.code}
              type="checkbox"
              inline
              label={role.name}
              checked={roleCodes.includes(role.code)}
              onChange={(e) => {
                if (e.target.checked) {
                  setRoleCodes((codes) => [...codes, role.code]);
                } else {
                  setRoleCodes((codes) => codes.filter((code) => code !== role.code));
                }
              }}
            />
          ))}
        </div>
      </Form.Group>

      <div className="d-flex justify-content-end gap-2">
        <Button variant="secondary" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" variant="primary" disabled={saving}>
          {saving ? "Saving..." : initial ? "Save changes" : "Create user"}
        </Button>
      </div>
    </Form>
  );
}

function UsersPage() {
  const [search, setSearch] = useState("");
  const [showCreate, setShowCreate] = useState(false);
  const [editing, setEditing] = useState(null);
  const { user: me, refresh: refreshMe } = useAuth();

  const {
    data,
    loading,
    error,
    refetch,
  } = useApi(() => fetchUsers({ search: search || undefined }), [search]);
  const { data: roles, error: rolesError } = useApi(() => fetchRoles(), []);

  const list = Array.isArray(data?.results) ? data.results : data || [];
  // Employee logins live outside the Users section (they are managed as
  // staff): hide EMPLOYEE-role accounts here and never offer the role
  // in the create/edit form.
  const isEmployeeLogin = (u) => (u.role_codes || []).includes("EMPLOYEE");
  const visibleList = list.filter((u) => !isEmployeeLogin(u));
  const formRoles = (roles || []).filter((r) => r.code !== "EMPLOYEE");
  const roleNames = Object.fromEntries(
    (roles || []).map((r) => [r.code, r.name])
  );

  const toggleActive = async (user) => {
    await updateUser(user.id, { is_active: !user.is_active });
    await refetch();
  };

  const handleDelete = async (user) => {
    if (
      !window.confirm(
        `Delete user ${user.email}? They will immediately lose access. This cannot be undone.`
      )
    ) {
      return;
    }
    await deleteUser(user.id);
    await refetch();
  };

  // Admin/superuser accounts are never deletable (backend enforces this
  // too): no delete button on their cards. Nobody may delete themselves.
  const canDelete = (user) => !user.is_superuser && user.id !== me?.id;

  return (
    <div>
      <div className="d-flex flex-wrap align-items-center justify-content-between mb-4">
        <div>
          <h4 className="mb-1">Users</h4>
          <p className="text-muted mb-0">Manage accounts and role assignments</p>
        </div>
        <Button variant="primary" onClick={() => setShowCreate(true)}>
          <i className="bi bi-plus-lg me-2" />
          New user
        </Button>
      </div>

      <Card className="shadow-sm">
        <Card.Header className="bg-white">
          <div className="d-flex justify-content-between align-items-center">
            <InputGroup style={{ maxWidth: 320 }}>
              <InputGroup.Text>
                <i className="bi bi-search" />
              </InputGroup.Text>
              <Form.Control
                placeholder="Search by name or email"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                aria-label="Search users"
              />
            </InputGroup>
            <span className="text-muted small">
              {visibleList.length} user(s)
            </span>
          </div>
        </Card.Header>
        <Card.Body>
          {loading ? (
            <LoadingSpinner label="Loading users..." />
          ) : error || rolesError ? (
            <Alert variant="danger" className="mb-0">
              Unable to load users. Check that the backend is running.
            </Alert>
          ) : visibleList.length === 0 ? (
            <div className="text-center text-muted py-4">No users found.</div>
          ) : (
            <Row className="g-3">
              {visibleList.map((user) => {
                const displayName = user.full_name || user.email;
                const initials = displayName
                  .split(/[\s@._-]+/)
                  .filter(Boolean)
                  .slice(0, 2)
                  .map((w) => w.charAt(0).toUpperCase())
                  .join("");
                return (
                  <Col xs={12} sm={6} xl={4} key={user.id}>
                    <Card className="shadow-sm h-100 overflow-hidden">
                      <div
                        className="position-relative"
                        style={{
                          height: 72,
                          background: user.is_superuser
                            ? "linear-gradient(135deg, #dc2626, #7c2d12)"
                            : "linear-gradient(135deg, #0ea5e9, #6366f1)",
                        }}
                      >
                        <span className="position-absolute top-0 end-0 m-2">
                          <Badge bg={user.is_active ? "success" : "secondary"}>
                            {user.is_active ? "Active" : "Inactive"}
                          </Badge>
                        </span>
                        {user.is_superuser && (
                          <span className="position-absolute top-0 start-0 m-2 badge bg-dark bg-opacity-50">
                            <i className="bi bi-shield-lock me-1" />
                            SUPERUSER
                          </span>
                        )}
                      </div>
                      <Card.Body className="pt-0">
                        <div
                          className="rounded-circle bg-primary text-white d-flex align-items-center justify-content-center fw-bold border border-3 border-white shadow-sm"
                          style={{ width: 52, height: 52, fontSize: "1.15rem", marginTop: -26 }}
                        >
                          {initials}
                        </div>
                        <div className="fw-semibold fs-5 mt-2 text-truncate">{displayName}</div>
                        <div className="text-muted small mb-2 text-truncate">{user.email}</div>
                        <div className="mb-2">
                          {user.role_codes?.length ? (
                            user.role_codes.map((code) => (
                              <Badge key={code} bg="light" text="dark" className="me-1 mb-1">
                                {roleNames[code] || code}
                              </Badge>
                            ))
                          ) : (
                            <span className="text-muted small">No roles assigned</span>
                          )}
                        </div>
                        {user.phone && (
                          <div className="small text-muted">
                            <i className="bi bi-telephone me-1" />
                            {user.phone}
                          </div>
                        )}
                      </Card.Body>
                      <Card.Footer className="bg-white d-flex gap-2">
                        <Button
                          size="sm"
                          variant="outline-secondary"
                          className="flex-fill"
                          onClick={() => setEditing(user)}
                        >
                          <i className="bi bi-pencil me-1" />
                          Edit
                        </Button>
                        <Button
                          size="sm"
                          variant={user.is_active ? "outline-warning" : "outline-success"}
                          className="flex-fill"
                          onClick={() => toggleActive(user)}
                        >
                          {user.is_active ? "Deactivate" : "Activate"}
                        </Button>
                        {canDelete(user) && (
                          <Button
                            size="sm"
                            variant="outline-danger"
                            className="flex-fill"
                            onClick={() => handleDelete(user)}
                          >
                            <i className="bi bi-trash me-1" />
                            Delete
                          </Button>
                        )}
                      </Card.Footer>
                    </Card>
                  </Col>
                );
              })}
            </Row>
          )}
        </Card.Body>
      </Card>

      <Modal show={showCreate || Boolean(editing)} onHide={() => { setShowCreate(false); setEditing(null); }}>
        <Modal.Header closeButton>
          <Modal.Title>{editing ? "Edit user" : "Create user"}</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <UserForm
            key={editing?.id ?? "new"}
            initial={editing}
            roles={formRoles}
            onCancel={() => { setShowCreate(false); setEditing(null); }}
            onSubmit={async (payload) => {
              if (editing) {
                await updateUser(editing.id, payload);
                // Editing your own account changes your permissions: reload
                // the session so menus and route guards update immediately.
                if (editing.id === me?.id) {
                  await refreshMe();
                }
              } else {
                await createUser(payload);
              }
              setShowCreate(false);
              setEditing(null);
              await refetch();
            }}
          />
        </Modal.Body>
      </Modal>
    </div>
  );
}

export default UsersPage;