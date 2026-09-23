import { useMemo, useState } from "react";
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
import { useAuth } from "../../context/AuthContext";
import { useApi } from "../../hooks/useApi";
import {
  MOVEMENT_TYPES,
  createStockMovement,
  fetchStockMovements,
  fetchWarehouses,
  movementApiLabel,
} from "../../services/inventoryService";
import { fetchProducts } from "../../services/productService";

const OUT_TYPES = new Set(["SALE", "PURCHASE_RETURN", "ADJUSTMENT_OUT", "TRANSFER_OUT"]);

function MovementForm({ products, warehouses, onCancel, onSubmit }) {
  const [movementType, setMovementType] = useState("PURCHASE");
  const [warehouse, setWarehouse] = useState("");
  const [destination, setDestination] = useState("");
  const [product, setProduct] = useState("");
  const [quantity, setQuantity] = useState("");
  const [movementDate, setMovementDate] = useState(new Date().toISOString().slice(0, 10));
  const [notes, setNotes] = useState("");
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  const isTransfer = movementType === "TRANSFER_OUT";

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setSaving(true);
    try {
      const payload = {
        movement_type: movementType,
        warehouse: Number(warehouse),
        product: Number(product),
        quantity,
        movement_date: movementDate,
        notes,
      };
      if (isTransfer) {
        payload.destination_warehouse = Number(destination);
      }
      await onSubmit(payload);
    } catch (err) {
      const errors = err?.response?.data?.errors || {};
      const first = Object.values(errors).flat()[0];
      setError(first || "Unable to record movement.");
    } finally {
      setSaving(false);
    }
  };

  const isOut = OUT_TYPES.has(movementType);
  return (
    <Form onSubmit={handleSubmit}>
      {error && <Alert variant="danger" className="py-2">{error}</Alert>}
      <Row>
        <Col md={6} className="mb-3">
          <Form.Group controlId="mv-type">
            <Form.Label>Movement type *</Form.Label>
            <Form.Select value={movementType} onChange={(e) => setMovementType(e.target.value)}>
              {MOVEMENT_TYPES.map((t) => (
                <option key={t} value={t}>{movementApiLabel(t)}</option>
              ))}
            </Form.Select>
          </Form.Group>
        </Col>
        <Col md={6} className="mb-3">
          <Form.Group controlId="mv-date">
            <Form.Label>Date *</Form.Label>
            <Form.Control type="date" value={movementDate} onChange={(e) => setMovementDate(e.target.value)} required />
          </Form.Group>
        </Col>
      </Row>

      <Row>
        <Col md={6} className="mb-3">
          <Form.Group controlId="mv-warehouse">
            <Form.Label>{isTransfer ? "From warehouse *" : "Warehouse *"}</Form.Label>
            <Form.Select value={warehouse} onChange={(e) => setWarehouse(e.target.value)} required>
              <option value="">-- Select --</option>
              {warehouses.map((w) => (
                <option key={w.id} value={String(w.id)}>
                  {w.warehouse_code} - {w.warehouse_name}
                </option>
              ))}
            </Form.Select>
          </Form.Group>
        </Col>
        {isTransfer && (
          <Col md={6} className="mb-3">
            <Form.Group controlId="mv-destination">
              <Form.Label>To warehouse *</Form.Label>
              <Form.Select value={destination} onChange={(e) => setDestination(e.target.value)} required>
                <option value="">-- Select --</option>
                {warehouses
                  .filter((w) => String(w.id) !== warehouse)
                  .map((w) => (
                    <option key={w.id} value={String(w.id)}>
                      {w.warehouse_code} - {w.warehouse_name}
                    </option>
                  ))}
              </Form.Select>
            </Form.Group>
          </Col>
        )}
      </Row>

      <Row>
        <Col md={8} className="mb-3">
          <Form.Group controlId="mv-product">
            <Form.Label>Product *</Form.Label>
            <Form.Select value={product} onChange={(e) => setProduct(e.target.value)} required>
              <option value="">-- Select --</option>
              {products.map((p) => (
                <option key={p.id} value={String(p.id)}>
                  {p.sku} - {p.product_name}
                </option>
              ))}
            </Form.Select>
          </Form.Group>
        </Col>
        <Col md={4} className="mb-3">
          <Form.Group controlId="mv-qty">
            <Form.Label>Quantity ({isOut ? "out" : "in"}) *</Form.Label>
            <Form.Control
              type="number"
              min="0.01"
              step="0.01"
              value={quantity}
              onChange={(e) => setQuantity(e.target.value)}
              required
            />
          </Form.Group>
        </Col>
      </Row>

      <Form.Group className="mb-4" controlId="mv-notes">
        <Form.Label>Notes</Form.Label>
        <Form.Control as="textarea" rows={2} value={notes} onChange={(e) => setNotes(e.target.value)} />
      </Form.Group>

      <div className="d-flex justify-content-end gap-2">
        <Button variant="secondary" onClick={onCancel}>Cancel</Button>
        <Button type="submit" variant="primary" disabled={saving}>
          {saving ? "Recording..." : "Record movement"}
        </Button>
      </div>
    </Form>
  );
}

function StockMovementsPage() {
  const { hasPerm } = useAuth();
  const [showForm, setShowForm] = useState(false);

  const { data, loading, error, refetch } = useApi(() => fetchStockMovements({}), []);
  const movements = useMemo(
    () => (Array.isArray(data?.results) ? data.results : data || []),
    [data]
  );

  const { data: productsData } = useApi(() => fetchProducts({ page_size: 100 }), []);
  const products = useMemo(
    () => (Array.isArray(productsData?.results) ? productsData.results : productsData || []),
    [productsData]
  );

  const { data: warehousesData } = useApi(() => fetchWarehouses({ page_size: 100 }), []);
  const warehouses = useMemo(
    () => (Array.isArray(warehousesData?.results) ? warehousesData.results : warehousesData || []),
    [warehousesData]
  );

  const canManage = hasPerm("stock_movements.manage");
  const isOut = (type) => OUT_TYPES.has(type);

  return (
    <div>
      <div className="d-flex flex-wrap align-items-center justify-content-between mb-4">
        <div>
          <h4 className="mb-1">Stock Movements</h4>
          <p className="text-muted mb-0">Every in/out/transfer that changes stock levels</p>
        </div>
        {canManage && (
          <Button variant="primary" onClick={() => setShowForm(true)}>
            <i className="bi bi-plus-lg me-2" />
            Record movement
          </Button>
        )}
      </div>

      <Card className="shadow-sm">
        <Card.Header className="bg-white">
          <div className="d-flex justify-content-between align-items-center">
            <InputGroup style={{ maxWidth: 260 }}>
              <InputGroup.Text><i className="bi bi-filter" /></InputGroup.Text>
              <Form.Control
                as="select"
                defaultValue=""
              >
                <option value="">All types</option>
                {MOVEMENT_TYPES.map((t) => (
                  <option key={t} value={t}>{movementApiLabel(t)}</option>
                ))}
              </Form.Control>
            </InputGroup>
            <span className="text-muted small">{data?.count ?? movements.length} movement(s)</span>
          </div>
        </Card.Header>
        <Card.Body>
          {loading ? (
            <LoadingSpinner label="Loading movements..." />
          ) : error ? (
            <Alert variant="danger" className="mb-0">Unable to load stock movements.</Alert>
          ) : (
            <Table hover responsive className="mb-0">
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Type</th>
                  <th>Product</th>
                  <th>From / To</th>
                  <th className="text-end">Qty</th>
                  <th>Notes</th>
                </tr>
              </thead>
              <tbody>
                {movements.map((m) => (
                  <tr key={m.id}>
                    <td className="text-muted">{m.movement_date}</td>
                    <td>
                      <Badge bg={isOut(m.movement_type) ? "warning" : "info"} text="dark">
                        {movementApiLabel(m.movement_type)}
                      </Badge>
                    </td>
                    <td>
                      <div className="fw-semibold">{m.product_name}</div>
                      <small className="text-muted">{m.product_sku}</small>
                    </td>
                    <td>
                      {m.destination_code
                        ? `${m.warehouse_name} → ${m.destination_code}`
                        : m.warehouse_name}
                    </td>
                    <td className={`text-end fw-semibold ${isOut(m.movement_type) ? "text-danger" : "text-success"}`}>
                      {isOut(m.movement_type) ? "−" : "+"}{m.quantity}
                    </td>
                    <td className="text-muted">{m.notes || "—"}</td>
                  </tr>
                ))}
                {movements.length === 0 && (
                  <tr>
                    <td colSpan={6} className="text-center text-muted py-4">
                      No movements recorded yet.
                    </td>
                  </tr>
                )}
              </tbody>
            </Table>
          )}
        </Card.Body>
      </Card>

      <Modal show={showForm} onHide={() => setShowForm(false)}>
        <Modal.Header closeButton>
          <Modal.Title>Record stock movement</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <MovementForm
            products={products}
            warehouses={warehouses}
            onCancel={() => setShowForm(false)}
            onSubmit={async (payload) => {
              await createStockMovement(payload);
              setShowForm(false);
              await refetch();
            }}
          />
        </Modal.Body>
      </Modal>
    </div>
  );
}

export default StockMovementsPage;