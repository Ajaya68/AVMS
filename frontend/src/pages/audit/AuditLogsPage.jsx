import { useEffect, useState } from "react";
import { Alert, Badge, Button, Card, Col, Form, Pagination, Row, Table } from "react-bootstrap";

import LoadingSpinner from "../../components/LoadingSpinner";
import { AUDIT_ACTIONS, fetchAuditLogs } from "../../services/auditLogService";

const ACTION_TONE = {
  CREATE: "text-bg-success",
  UPDATE: "text-bg-info",
  DELETE: "text-bg-danger",
  LOGIN: "text-bg-secondary",
  LOGOUT: "text-bg-secondary",
  SALE_CREATED: "text-bg-success",
  PURCHASE_CREATED: "text-bg-info",
  PAYMENT_CREATED: "text-bg-success",
  STOCK_ADJUSTED: "text-bg-warning",
  USER_CREATED: "text-bg-secondary",
};

function formatDate(value) {
  if (!value) return "";
  return new Date(value).toLocaleString();
}

export default function AuditLogsPage() {
  const [filters, setFilters] = useState({ module: "", action: "", search: "", from: "", to: "" });
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [page, setPage] = useState(1);

  const PER_PAGE = 20;

  async function load(nextPage = 1) {
    setLoading(true);
    setError("");
    try {
      const params = { page: nextPage, page_size: PER_PAGE, ...Object.fromEntries(Object.entries(filters).filter(([, v]) => v !== "")) };
      setData(await fetchAuditLogs(params));
      setPage(nextPage);
    } catch (e) {
      setError(e?.response?.data?.message || "Failed to load audit logs");
    } finally {
      setLoading(false);
    }
  }

  const results = data?.results || [];
  const totalPages = Math.max(Math.ceil((data?.count || 0) / PER_PAGE), 1);
  const current = Math.min(page, totalPages);

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <Card>
      <Card.Header>Audit Logs</Card.Header>
      <Card.Body>
        <Row className="g-2 mb-3 align-items-end">
          <Col md={2}>
            <Form.Label className="small text-muted mb-0">Module</Form.Label>
            <Form.Control
              as="select"
              value={filters.module}
              onChange={(e) => setFilters({ ...filters, module: e.target.value })}
            >
              <option value="">All</option>
              {[...new Set((data?.results || []).map((r) => r.module))].map((m) => (
                <option key={m} value={m}>{m}</option>
              ))}
            </Form.Control>
          </Col>
          <Col md={2}>
            <Form.Label className="small text-muted mb-0">Action</Form.Label>
            <Form.Control
              as="select"
              value={filters.action}
              onChange={(e) => setFilters({ ...filters, action: e.target.value })}
            >
              <option value="">All</option>
              {AUDIT_ACTIONS.map((a) => (
                <option key={a} value={a}>{a}</option>
              ))}
            </Form.Control>
          </Col>
          <Col md={2}>
            <Form.Label className="small text-muted mb-0">From</Form.Label>
            <Form.Control type="date" value={filters.from} onChange={(e) => setFilters({ ...filters, from: e.target.value })} />
          </Col>
          <Col md={2}>
            <Form.Label className="small text-muted mb-0">To</Form.Label>
            <Form.Control type="date" value={filters.to} onChange={(e) => setFilters({ ...filters, to: e.target.value })} />
          </Col>
          <Col md={2}>
            <Form.Label className="small text-muted mb-0">Search</Form.Label>
            <Form.Control
              placeholder="Description / email"
              value={filters.search}
              onChange={(e) => setFilters({ ...filters, search: e.target.value })}
              onKeyDown={(e) => e.key === "Enter" && load(1)}
            />
          </Col>
          <Col md={2}>
            <Button variant="primary" disabled={loading} onClick={() => load(1)} className="w-100">
              Apply
            </Button>
          </Col>
        </Row>

        {error && <Alert variant="danger">{error}</Alert>}
        {loading && <LoadingSpinner />}
        {!loading && !error && (
          <>
            <Table size="sm" striped hover responsive>
              <thead>
                <tr>
                  <th>#</th><th>Action</th><th>Module</th><th>Object</th>
                  <th>User</th><th>IP</th><th>Description</th><th>When</th>
                </tr>
              </thead>
              <tbody>
                {results.map((r) => (
                  <tr key={r.id}>
                    <td className="text-muted">{r.id}</td>
                    <td>
                      <Badge bg="" className={ACTION_TONE[r.action] || "text-bg-secondary"}>
                        {r.action}
                      </Badge>
                    </td>
                    <td>{r.module}</td>
                    <td>
                      {r.object_type || "-"}
                      {r.object_id ? <span className="text-muted small"> #{r.object_id}</span> : null}
                    </td>
                    <td>{r.user_email || "-"}</td>
                    <td>{r.ip_address || "-"}</td>
                    <td className="text-muted">{r.description}</td>
                    <td className="text-muted text-nowrap">{formatDate(r.created_at)}</td>
                  </tr>
                ))}
                {results.length === 0 && (
                  <tr><td colSpan={8} className="text-muted text-center py-3">No audit entries match</td></tr>
                )}
              </tbody>
            </Table>
            {totalPages > 1 && (
              <Pagination size="sm">
                <Pagination.Prev disabled={current <= 1} onClick={() => load(current - 1)} />
                {[...Array(totalPages)].slice(0, 10).map((_, i) => (
                  <Pagination.Item key={i + 1} active={current === i + 1} onClick={() => load(i + 1)}>
                    {i + 1}
                  </Pagination.Item>
                ))}
                <Pagination.Next disabled={current >= totalPages} onClick={() => load(current + 1)} />
              </Pagination>
            )}
          </>
        )}
      </Card.Body>
    </Card>
  );
}