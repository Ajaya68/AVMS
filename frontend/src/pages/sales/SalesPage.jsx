import { useCallback, useMemo, useState } from "react";
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
import { getActiveVentureId } from "../../services/api";
import { fetchCustomers } from "../../services/customerService";
import { fetchWarehouses } from "../../services/inventoryService";
import { fetchProducts } from "../../services/productService";
import {
  SALE_STATUSES,
  createSale,
  deleteSale,
  fetchSales,
  saleStatusLabel,
} from "../../services/saleService";
import SaleReturnForm from "./SaleReturnForm";

function money(n) {
  return Number(n || 0).toFixed(2);
}

function statusColor(status) {
  return {
    COMPLETED: "success",
    PARTIAL: "warning",
    RETURNED: "secondary",
    PENDING: "info",
    CANCELLED: "danger",
  }[status] || "light";
}

function SaleForm({ products, customers, warehouses, onCancel, onSaved }) {
  const [customer, setCustomer] = useState("");
  const [warehouse, setWarehouse] = useState("");
  const [saleDate, setSaleDate] = useState(new Date().toISOString().slice(0, 10));
  const [lines, setLines] = useState([
    { product: "", quantity: "", unit_price: "", discount: "0", tax: "0" },
  ]);
  const [discount, setDiscount] = useState("0");
  const [billTax, setBillTax] = useState("0");
  const [paid, setPaid] = useState("0");
  const [notes, setNotes] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  const subtotal = lines.reduce(
    (s, l) => s + Number(l.quantity || 0) * Number(l.unit_price || 0),
    0
  );
  const total = subtotal - Number(discount || 0) + Number(billTax || 0);
  const due = total - Number(paid || 0);

  const addLine = () =>
    setLines((prev) => [
      ...prev,
      { product: "", quantity: "", unit_price: "", discount: "0", tax: "0" },
    ]);

  const updateLine = (index, patch) =>
    setLines((prev) => prev.map((l, i) => (i === index ? { ...l, ...patch } : l)));

  const handleProductChange = (index, productId) => {
    const product = products.find((p) => p.id === Number(productId));
    updateLine(index, {
      product: productId,
      unit_price: product ? product.selling_price : "",
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const items = lines
      .filter((l) => Number(l.quantity) > 0 && Number(l.unit_price) > 0)
      .map((l) => ({
        product: Number(l.product),
        quantity: l.quantity,
        unit_price: l.unit_price,
        discount: l.discount,
        tax: l.tax,
      }));
    if (items.length === 0) {
      setError("Add at least one item with quantity and unit price.");
      return;
    }
    setSaving(true);
    setError("");
    try {
      await createSale({
        venture: Number(getActiveVentureId()),
        customer: Number(customer),
        warehouse: warehouse ? Number(warehouse) : null,
        sale_date: saleDate,
        discount,
        tax: billTax,
        paid_amount: paid,
        notes,
        items,
      });
      onSaved();
    } catch (err) {
      const errors = err?.response?.data?.errors || {};
      const first = Object.values(errors).flat()[0];
      setError(first || err?.response?.data?.message || "Unable to create sale.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <Form onSubmit={handleSubmit}>
      {error && <Alert variant="danger" className="py-2">{error}</Alert>}
      <Row>
        <Col md={4} className="mb-3">
          <Form.Group controlId="si-customer">
            <Form.Label>Customer *</Form.Label>
            <Form.Select value={customer} onChange={(e) => setCustomer(e.target.value)} required>
              <option value="">-- Select --</option>
              {customers.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.customer_code} - {c.name}
                </option>
              ))}
            </Form.Select>
          </Form.Group>
        </Col>
        <Col md={4} className="mb-3">
          <Form.Group controlId="si-warehouse">
            <Form.Label>Dispatch from</Form.Label>
            <Form.Select value={warehouse} onChange={(e) => setWarehouse(e.target.value)}>
              <option value="">-- Later --</option>
              {warehouses.map((w) => (
                <option key={w.id} value={w.id}>
                  {w.warehouse_code} - {w.warehouse_name}
                </option>
              ))}
            </Form.Select>
          </Form.Group>
        </Col>
        <Col md={4} className="mb-3">
          <Form.Group controlId="si-date">
            <Form.Label>Sale date *</Form.Label>
            <Form.Control
              type="date"
              value={saleDate}
              onChange={(e) => setSaleDate(e.target.value)}
              required
            />
          </Form.Group>
        </Col>
      </Row>

      <Card className="mb-3">
        <Card.Header className="bg-light">Items</Card.Header>
        <Card.Body className="p-0">
          <Table hover responsive className="mb-0 align-middle">
            <thead>
              <tr>
                <th>Product *</th>
                <th style={{ width: 110 }}>Qty *</th>
                <th style={{ width: 130 }}>Unit price *</th>
                <th style={{ width: 100 }}>Disc</th>
                <th style={{ width: 90 }}>Tax %</th>
                <th className="text-end">Total</th>
                <th style={{ width: 40 }}></th>
              </tr>
            </thead>
            <tbody>
              {lines.map((l, i) => (
                <tr key={i}>
                  <td>
                    <Form.Select
                      value={l.product}
                      onChange={(e) => handleProductChange(i, e.target.value)}
                      required
                    >
                      <option value="">-- Select --</option>
                      {products.map((p) => (
                        <option key={p.id} value={String(p.id)}>
                          {p.sku} - {p.product_name}
                        </option>
                      ))}
                    </Form.Select>
                  </td>
                  <td>
                    <Form.Control
                      type="number"
                      step="0.01"
                      min="0.01"
                      value={l.quantity}
                      onChange={(e) => updateLine(i, { quantity: e.target.value })}
                      required
                    />
                  </td>
                  <td>
                    <Form.Control
                      type="number"
                      step="0.01"
                      min="0.01"
                      value={l.unit_price}
                      onChange={(e) => updateLine(i, { unit_price: e.target.value })}
                      required
                    />
                  </td>
                  <td>
                    <Form.Control
                      type="number"
                      step="0.01"
                      value={l.discount}
                      onChange={(e) => updateLine(i, { discount: e.target.value })}
                    />
                  </td>
                  <td>
                    <Form.Control
                      type="number"
                      step="0.01"
                      value={l.tax}
                      onChange={(e) => updateLine(i, { tax: e.target.value })}
                    />
                  </td>
                  <td className="text-end fw-semibold">
                    {money(Number(l.quantity || 0) * Number(l.unit_price || 0))}
                  </td>
                  <td>
                    <Button
                      variant="link"
                      size="sm"
                      className="text-danger p-0"
                      onClick={() => setLines((prev) => prev.filter((_, x) => x !== i))}
                      disabled={lines.length === 1}
                    >
                      <i className="bi bi-trash" />
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </Table>
          <div className="p-2">
            <Button variant="outline-primary" size="sm" onClick={addLine}>
              <i className="bi bi-plus-lg me-1" /> Add item
            </Button>
          </div>
        </Card.Body>
      </Card>

      <Row>
        <Col md={3} className="mb-3">
          <Form.Group controlId="si-discount">
            <Form.Label>Bill discount (₹)</Form.Label>
            <Form.Control
              type="number"
              step="0.01"
              value={discount}
              onChange={(e) => setDiscount(e.target.value)}
            />
          </Form.Group>
        </Col>
        <Col md={3} className="mb-3">
          <Form.Group controlId="si-tax">
            <Form.Label>Bill tax (₹)</Form.Label>
            <Form.Control
              type="number"
              step="0.01"
              value={billTax}
              onChange={(e) => setBillTax(e.target.value)}
            />
          </Form.Group>
        </Col>
        <Col md={3} className="mb-3">
          <Form.Group controlId="si-paid">
            <Form.Label>Received now (₹)</Form.Label>
            <Form.Control
              type="number"
              step="0.01"
              value={paid}
              onChange={(e) => setPaid(e.target.value)}
            />
          </Form.Group>
        </Col>
        <Col md={3} className="mb-3">
          <div className="d-flex flex-column align-items-end">
            <span className="text-muted small">Subtotal</span>
            <span className="fs-5 fw-bold">₹{money(subtotal)}</span>
            <span className="text-muted small">Due from customer</span>
            <span className="fw-semibold text-danger">₹{money(Math.max(due, 0))}</span>
          </div>
        </Col>
      </Row>

      <Form.Group className="mb-4" controlId="si-notes">
        <Form.Label>Notes</Form.Label>
        <Form.Control
          as="textarea"
          rows={2}
          value={notes}
          onChange={(e) => setNotes(e.target.value)}
        />
      </Form.Group>

      <div className="d-flex justify-content-end gap-2">
        <Button variant="secondary" onClick={onCancel}>Cancel</Button>
        <Button type="submit" variant="primary" disabled={saving}>
          {saving ? "Recording..." : "Record sale"}
        </Button>
      </div>
    </Form>
  );
}

function SaleDetail({ sale, onDelete, onReturn, canManage }) {
  if (!sale) return null;
  return (
    <div>
      <div className="d-flex justify-content-between align-items-start mb-2">
        <div>
          <h5 className="mb-1">{sale.customer_name}</h5>
          <span className="text-muted">{sale.customer_code}</span>
          <div>
            <Badge bg={statusColor(sale.status)}>
              {saleStatusLabel(sale.status)}
            </Badge>
          </div>
        </div>
        <div className="text-end">
          <div className="fw-bold">{sale.invoice_number}</div>
          <span className="text-muted small">{sale.sale_date}</span>
          <div className="text-muted small">{sale.warehouse_code || "No warehouse"}</div>
        </div>
      </div>

      <Table hover responsive className="align-middle">
        <thead>
          <tr>
            <th>Product</th>
            <th className="text-end">Qty</th>
            <th className="text-end">Unit price</th>
            <th className="text-end">Amount</th>
          </tr>
        </thead>
        <tbody>
          {(sale.items || []).map((it) => (
            <tr key={it.id}>
              <td>
                <div className="fw-semibold">{it.product_name}</div>
                <small className="text-muted">{it.product_sku}</small>
              </td>
              <td className="text-end">{it.quantity}</td>
              <td className="text-end">₹{money(it.unit_price)}</td>
              <td className="text-end fw-semibold">₹{money(it.total)}</td>
            </tr>
          ))}
        </tbody>
      </Table>

      <div className="d-flex justify-content-end">
        <div className="text-end" style={{ minWidth: 220 }}>
          <div className="d-flex justify-content-between">
            <span className="text-muted">Subtotal</span>
            <span>₹{money(sale.subtotal)}</span>
          </div>
          <div className="d-flex justify-content-between">
            <span className="text-muted">Discount</span>
            <span>−₹{money(sale.discount)}</span>
          </div>
          <div className="d-flex justify-content-between">
            <span className="text-muted">Tax</span>
            <span>+₹{money(sale.tax)}</span>
          </div>
          <div className="d-flex justify-content-between fw-bold border-top pt-1">
            <span>Total</span>
            <span>₹{money(sale.total_amount)}</span>
          </div>
          <div className="d-flex justify-content-between">
            <span className="text-success">Received</span>
            <span>₹{money(sale.paid_amount)}</span>
          </div>
          {Number(sale.returned_amount) > 0 && (
            <div className="d-flex justify-content-between">
              <span className="text-warning">Returned</span>
              <span>₹{money(sale.returned_amount)}</span>
            </div>
          )}
          <div className="d-flex justify-content-between fw-semibold">
            <span className="text-danger">Due</span>
            <span className="text-danger">₹{money(sale.due_amount)}</span>
          </div>
        </div>
      </div>

      <div className="d-flex justify-content-end gap-2 mt-3">
        {canManage && (
          <>
            <Button
              variant="outline-danger"
              onClick={onDelete}
              disabled={Boolean(sale.warehouse_code)}
              title={sale.warehouse_code ? "Stock dispatched - reverse with a return" : "Delete"}
            >
              Delete
            </Button>
            <Button variant="outline-warning" onClick={onReturn}>
              Return goods
            </Button>
          </>
        )}
      </div>
    </div>
  );
}

function SalesPage() {
  const { hasPerm } = useAuth();
  const [showForm, setShowForm] = useState(false);
  const [showReturn, setShowReturn] = useState(false);
  const [selected, setSelected] = useState(null);
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [deleteError, setDeleteError] = useState("");

  const { data, loading, error, refetch } = useApi(
    () => fetchSales({ search, status: statusFilter }),
    [search, statusFilter]
  );
  const sales = useMemo(
    () => (Array.isArray(data?.results) ? data.results : data || []),
    [data]
  );

  const { data: productsData } = useApi(() => fetchProducts({ page_size: 100 }), []);
  const products = useMemo(
    () => (Array.isArray(productsData?.results) ? productsData.results : productsData || []),
    [productsData]
  );
  const { data: customersData } = useApi(() => fetchCustomers({ page_size: 100 }), []);
  const customers = useMemo(
    () => (Array.isArray(customersData?.results) ? customersData.results : customersData || []),
    [customersData]
  );
  const { data: warehousesData } = useApi(() => fetchWarehouses({ page_size: 100 }), []);
  const warehouses = useMemo(
    () => (Array.isArray(warehousesData?.results) ? warehousesData.results : warehousesData || []),
    [warehousesData]
  );

  const canManage = hasPerm("sales.manage");
  const canViewReturns = hasPerm("sales_returns.view");

  const openDetail = (s) => {
    setDeleteError("");
    setSelected(s);
  };

  const handleDelete = async () => {
    setDeleteError("");
    try {
      await deleteSale(selected.id);
      setSelected(null);
      await refetch();
    } catch (err) {
      setDeleteError(err?.response?.data?.message || "Unable to delete sale.");
    }
  };

  const refresh = useCallback(async () => {
    await refetch();
    setSelected(null);
    setShowForm(false);
    setShowReturn(false);
  }, [refetch]);

  return (
    <div>
      <div className="d-flex flex-wrap align-items-center justify-content-between mb-4">
        <div>
          <h4 className="mb-1">Sales</h4>
          <p className="text-muted mb-0">Bills to customers; goods dispatched from a warehouse</p>
        </div>
        {canManage && (
          <Button variant="primary" onClick={() => setShowForm(true)}>
            <i className="bi bi-plus-lg me-2" />
            New sale
          </Button>
        )}
      </div>

      <Card className="shadow-sm">
        <Card.Header className="bg-white">
          <div className="d-flex flex-wrap gap-2">
            <InputGroup style={{ maxWidth: 280 }}>
              <InputGroup.Text><i className="bi bi-search" /></InputGroup.Text>
              <Form.Control
                placeholder="Search invoice or customer"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </InputGroup>
            <Form.Select
              style={{ maxWidth: 180 }}
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
            >
              <option value="">All statuses</option>
              {SALE_STATUSES.map((s) => (
                <option key={s} value={s}>{saleStatusLabel(s)}</option>
              ))}
            </Form.Select>
            <span className="ms-auto text-muted small align-self-center">
              {data?.count ?? sales.length} sale(s)
            </span>
          </div>
        </Card.Header>
        <Card.Body>
          {loading ? (
            <LoadingSpinner label="Loading sales..." />
          ) : error ? (
            <Alert variant="danger" className="mb-0">Unable to load sales.</Alert>
          ) : (
            <Table hover responsive className="mb-0">
              <thead>
                <tr>
                  <th>Invoice</th>
                  <th>Date</th>
                  <th>Customer</th>
                  <th>Status</th>
                  <th className="text-end">Total</th>
                  <th className="text-end">Due</th>
                  <th className="text-end">Returned</th>
                </tr>
              </thead>
              <tbody>
                {sales.map((s) => (
                  <tr key={s.id} onClick={() => openDetail(s)} className="cursor-pointer">
                    <td className="fw-semibold">{s.invoice_number}</td>
                    <td className="text-muted">{s.sale_date}</td>
                    <td>{s.customer_name}</td>
                    <td>
                      <Badge bg={statusColor(s.status)}>
                        {saleStatusLabel(s.status)}
                      </Badge>
                    </td>
                    <td className="text-end">₹{money(s.total_amount)}</td>
                    <td className="text-end text-danger">₹{money(s.due_amount)}</td>
                    <td className="text-end text-warning">
                      {Number(s.returned_amount) > 0 ? `₹${money(s.returned_amount)}` : "—"}
                    </td>
                  </tr>
                ))}
                {sales.length === 0 && (
                  <tr>
                    <td colSpan={7} className="text-center text-muted py-4">
                      No sales found.
                    </td>
                  </tr>
                )}
              </tbody>
            </Table>
          )}
        </Card.Body>
      </Card>

      <Modal show={showForm} onHide={() => setShowForm(false)} size="lg">
        <Modal.Header closeButton>
          <Modal.Title>New sale</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <SaleForm
            products={products}
            customers={customers}
            warehouses={warehouses}
            onCancel={() => setShowForm(false)}
            onSaved={refresh}
          />
        </Modal.Body>
      </Modal>

      <Modal show={Boolean(selected) && !showReturn} onHide={() => setSelected(null)} size="lg">
        <Modal.Header closeButton>
          <Modal.Title>Sale detail</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {deleteError && <Alert variant="danger" className="py-2">{deleteError}</Alert>}
          <SaleDetail
            sale={selected}
            onDelete={handleDelete}
            onReturn={() => setShowReturn(true)}
            canManage={canManage}
          />
        </Modal.Body>
      </Modal>

      {canViewReturns && (
        <Modal show={showReturn} onHide={() => setShowReturn(false)} size="lg">
          <Modal.Header closeButton>
            <Modal.Title>Return goods — {selected?.invoice_number}</Modal.Title>
          </Modal.Header>
          <Modal.Body>
            <SaleReturnForm
              preselectSaleId={selected?.id}
              onCancel={() => setShowReturn(false)}
              onSaved={refresh}
            />
          </Modal.Body>
        </Modal>
      )}
    </div>
  );
}

export default SalesPage;