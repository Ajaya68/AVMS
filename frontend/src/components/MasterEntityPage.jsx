import { useMemo, useState } from "react";
import {
  Alert,
  Button,
  Card,
  Col,
  Form,
  InputGroup,
  Modal,
  Row,
  Table,
} from "react-bootstrap";

import LoadingSpinner from "./LoadingSpinner";
import { useAuth } from "../context/AuthContext";
import { useApi } from "../hooks/useApi";
import { fetchVentures } from "../services/ventureService";

function buildFormValues(fields, row) {
  const values = {};
  for (const field of fields) {
    const source = row?.[field.name] ?? "";
    values[field.name] =
      field.type === "number" && source !== "" && source !== null
        ? String(source)
        : source;
  }
  return values;
}

function MasterEntityForm({
  fields,
  initial,
  initialValues,
  ventureOptions,
  lookups,
  codeField,
  onSubmit,
  onCancel,
}) {
  const [values, setValues] = useState(() =>
    initialValues ?? buildFormValues(fields, initial)
  );
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  const set = (name) => (e) =>
    setValues((v) => ({ ...v, [name]: e.target.value }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setSaving(true);
    try {
      const payload = { ...values };
      for (const field of fields) {
        if (field.type === "number") {
          payload[field.name] =
            payload[field.name] === "" ? null : payload[field.name];
        }
      }
      await onSubmit(payload);
    } catch (err) {
      const errors = err?.response?.data?.errors || {};
      const first = Object.values(errors).flat()[0];
      setError(first || "Unable to save.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <Form onSubmit={handleSubmit}>
      {error && <Alert variant="danger" className="py-2">{error}</Alert>}
      <Row>
        {fields.map((field) => {
          const isVenture = field.type === "venture";
          const isSelect = field.type === "select";
          const isTextarea = field.type === "textarea";
const options = isVenture
    ? ventureOptions.map((v) => ({
        value: v.id,
        label: `${v.venture_code} - ${v.venture_name}`,
      }))
    : field.options || lookups?.[field.name] || [];
          return (
            <Col md={field.fullWidth ? 12 : 6} key={field.name} className="mb-3">
              <Form.Group controlId={field.name}>
                <Form.Label>{field.label}{field.required && " *"}</Form.Label>
                {isVenture || isSelect ? (
                  <Form.Select
                    value={String(values[field.name] ?? "")}
                    onChange={set(field.name)}
                    required={field.required}
                  >
                    <option value="">-- Select --</option>
                    {options.map((opt) => (
                      <option key={opt.value} value={String(opt.value)}>
                        {opt.label}
                      </option>
                    ))}
                  </Form.Select>
                ) : (
                  <Form.Control
                    as={isTextarea ? "textarea" : "input"}
                    type={
                      isTextarea
                        ? undefined
                        : field.type === "number"
                        ? "number"
                        : field.type === "email"
                        ? "email"
                        : "text"
                    }
                    rows={isTextarea ? 2 : undefined}
                    value={values[field.name] ?? ""}
                    onChange={set(field.name)}
                    required={field.required}
                  />
                )}
                {isVenture && (
                  <Form.Text className="text-muted">
                    Records can be isolated per venture later; pick the owning venture.
                  </Form.Text>
                )}
              </Form.Group>
            </Col>
          );
        })}
      </Row>
      {codeField && (
        <div className="text-muted small mb-3">
          {initial?.[codeField]
            ? `${codeField.replace(/_/g, " ")}: ${initial[codeField]}`
            : "Code auto-generated on save"}
        </div>
      )}
      <div className="d-flex justify-content-end gap-2">
        <Button variant="secondary" onClick={onCancel}>Cancel</Button>
        <Button type="submit" variant="primary" disabled={saving}>
          {saving ? "Saving..." : initial ? "Save changes" : "Create"}
        </Button>
      </div>
    </Form>
  );
}

function MasterEntityPage({
  title,
  subtitle,
  singular,
  perm,
  service,
  columns,
  fields,
  codeField,
  initialValues,
  lookups,
  searchPlaceholder = "Search...",
}) {
  const { hasPerm } = useAuth();
  const [search, setSearch] = useState("");
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState(null);

  const { data, loading, error, refetch } = useApi(
    () => service.fetch({ search: search || undefined }),
    [search]
  );

  const list = useMemo(
    () => (Array.isArray(data?.results) ? data.results : data || []),
    [data]
  );
  const canManage = hasPerm(perm);

  const { data: venturesData } = useApi(() => fetchVentures({ page_size: 100 }), []);
  const ventureOptions = useMemo(
    () => (Array.isArray(venturesData?.results) ? venturesData.results : venturesData || []),
    [venturesData]
  );

  const handleDelete = async (row) => {
    if (
      window.confirm(
        `Delete ${singular} ${row[columns[0]?.key] ?? row.id}? This cannot be undone.`
      )
    ) {
      await service.remove(row.id);
      await refetch();
    }
  };

  return (
    <div>
      <div className="d-flex flex-wrap align-items-center justify-content-between mb-4">
        <div>
          <h4 className="mb-1">{title}</h4>
          <p className="text-muted mb-0">{subtitle}</p>
        </div>
        {canManage && (
          <Button
            variant="primary"
            onClick={() => { setEditing(null); setShowForm(true); }}
          >
            <i className="bi bi-plus-lg me-2" />
            New {singular}
          </Button>
        )}
      </div>

      <Card className="shadow-sm">
        <Card.Header className="bg-white">
          <div className="d-flex justify-content-between align-items-center">
            <InputGroup style={{ maxWidth: 320 }}>
              <InputGroup.Text><i className="bi bi-search" /></InputGroup.Text>
              <Form.Control
                placeholder={searchPlaceholder}
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </InputGroup>
            <span className="text-muted small">{data?.count ?? list.length} record(s)</span>
          </div>
        </Card.Header>
        <Card.Body>
          {loading ? (
            <LoadingSpinner label={`Loading ${title.toLowerCase()}...`} />
          ) : error ? (
            <Alert variant="danger" className="mb-0">
              Unable to load {title.toLowerCase()}.
            </Alert>
          ) : (
            <Table hover responsive className="mb-0">
              <thead>
                <tr>
                  {columns.map((c) => (
                    <th key={c.key}>{c.label}</th>
                  ))}
                  <th className="text-end">Actions</th>
                </tr>
              </thead>
              <tbody>
                {list.map((row) => (
                  <tr key={row.id}>
                    {columns.map((c) => (
                      <td key={c.key}>
                        {c.render ? c.render(row) : row[c.key] ?? "—"}
                      </td>
                    ))}
                    <td className="text-end">
                      {canManage && (
                        <>
                          <Button
                            size="sm"
                            variant="outline-secondary"
                            className="me-2"
                            onClick={() => { setEditing(row); setShowForm(true); }}
                          >
                            <i className="bi bi-pencil" />
                          </Button>
                          <Button
                            size="sm"
                            variant="outline-danger"
                            onClick={() => handleDelete(row)}
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
                    <td colSpan={columns.length + 1} className="text-center text-muted py-4">
                      No {title.toLowerCase()} found.
                    </td>
                  </tr>
                )}
              </tbody>
            </Table>
          )}
        </Card.Body>
      </Card>

      <Modal show={showForm} onHide={() => { setShowForm(false); setEditing(null); }}>
        <Modal.Header closeButton>
          <Modal.Title>{editing ? `Edit ${singular}` : `New ${singular}`}</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <MasterEntityForm
            key={editing?.id ?? "new"}
            fields={fields}
            initial={editing}
            initialValues={initialValues}
            ventureOptions={ventureOptions}
            lookups={lookups}
            codeField={codeField}
            onCancel={() => { setShowForm(false); setEditing(null); }}
            onSubmit={async (payload) => {
              if (editing) {
                await service.update(editing.id, payload);
              } else {
                await service.create(payload);
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

export default MasterEntityPage;