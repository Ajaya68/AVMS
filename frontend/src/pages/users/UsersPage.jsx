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

import LoadingSpinner from "../../components/LoadingSpinner";
import { useApi } from "../../hooks/useApi";
import { createUser, fetchRoles, fetchUsers, updateUser } from "../../services/userService";

function UserForm({ initial, roles, onSubmit, onCancel }) {
  const [email, setEmail] = useState(initial?.email ?? "");
  const [password, setPassword] = useState("");
  const [fullName, setFullName] = useState(initial?.full_name ?? "");
  const [phone, setPhone] = useState(initial?.phone ?? "");
  const [roleIds, setRoleIds] = useState(
    initial?.roles?.map((r) => r.id) ?? []
  );
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
        roles: roleIds,
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
              key={role.id}
              type="checkbox"
              inline
              label={role.name}
              checked={roleIds.includes(role.id)}
              onChange={(e) => {
                if (e.target.checked) {
                  setRoleIds((ids) => [...ids, role.id]);
                } else {
                  setRoleIds((ids) => ids.filter((id) => id !== role.id));
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

  const {
    data,
    loading,
    error,
    refetch,
  } = useApi(() => fetchUsers({ search: search || undefined }), [search]);
  const { data: roles, error: rolesError } = useApi(() => fetchRoles(), []);

  const list = Array.isArray(data?.results) ? data.results : data || [];

  const toggleActive = async (user) => {
    await updateUser(user.id, { is_active: !user.is_active });
    await refetch();
  };

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
              {data?.count ?? list.length} user(s)
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
          ) : (
            <Table hover responsive className="mb-0">
              <thead>
                <tr>
                  <th>User</th>
                  <th>Contact</th>
                  <th>Roles</th>
                  <th>Status</th>
                  <th className="text-end">Actions</th>
                </tr>
              </thead>
              <tbody>
                {list.map((user) => (
                  <tr key={user.id}>
                    <td>
                      <div className="fw-semibold">{user.full_name || "—"}</div>
                      <small className="text-muted">{user.email}</small>
                    </td>
                    <td className="text-muted">{user.phone || "—"}</td>
                    <td>
                      {user.roles?.length ? (
                        user.roles.map((r) => (
                          <Badge key={r.id} bg="light" text="dark" className="me-1">
                            {r.name}
                          </Badge>
                        ))
                      ) : (
                        <span className="text-muted">—</span>
                      )}
                    </td>
                    <td>
                      <Badge bg={user.is_active ? "success" : "secondary"}>
                        {user.is_active ? "Active" : "Inactive"}
                      </Badge>
                    </td>
                    <td className="text-end">
                      <Button
                        size="sm"
                        variant="outline-secondary"
                        onClick={() => setEditing(user)}
                        className="me-2"
                      >
                        <i className="bi bi-pencil" />
                      </Button>
                      <Button
                        size="sm"
                        variant={user.is_active ? "outline-danger" : "outline-success"}
                        onClick={() => toggleActive(user)}
                      >
                        {user.is_active ? "Deactivate" : "Activate"}
                      </Button>
                    </td>
                  </tr>
                ))}
                {list.length === 0 && (
                  <tr>
                    <td colSpan={5} className="text-center text-muted py-4">
                      No users found.
                    </td>
                  </tr>
                )}
              </tbody>
            </Table>
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
            roles={roles || []}
            onCancel={() => { setShowCreate(false); setEditing(null); }}
            onSubmit={async (payload) => {
              if (editing) {
                await updateUser(editing.id, payload);
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