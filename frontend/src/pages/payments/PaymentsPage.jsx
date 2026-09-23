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
import { getActiveVentureId } from "../../services/api";
import { fetchPurchases } from "../../services/purchaseService";
import { fetchSales } from "../../services/saleService";
import {
  PAYMENT_METHODS,
  PAYMENT_TYPES,
  createPayment,
  deletePayment,
  fetchPayments,
  label,
  typeBadge,
} from "../../services/paymentService";

function money(n) {
  return Number(n || 0).toFixed(2);
}

function PaymentForm({ sales, purchases, onCancel, onSaved }) {
  const [payType, setPayType] = useState("RECEIVED");
  const [referenceId, setReferenceId] = useState("");
  const [amount, setAmount] = useState("");
  const [paymentDate, setPaymentDate] = useState(new Date().toISOString().slice(0, 10));
  const [method, setMethod] = useState("CASH");
  const [txnRef, setTxnRef] = useState("");
  const [notes, setNotes] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  const isReceived = payType === "RECEIVED";
  const references = isReceived
    ? sales.filter((s) => Number(s.due_amount) > 0)
    : purchases.filter((p) => Number(p.due_amount) > 0);

  const selected = references.find((r) => String(r.id) === referenceId);
  const maxDue = selected ? Number(selected.due_amount) : 0;

  const handleTypeChange = (type) => {
    setPayType(type);
    setReferenceId("");
    setAmount("");
  };

  const handleReferenceChange = (e) => {
    const id = e.target.value;
    setReferenceId(id);
    const row = references.find((r) => String(r.id) === id);
    if (row) setAmount(String(row.due_amount));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError("");
    try {
      await createPayment({
        venture: Number(getActiveVentureId()),
        payment_type: payType,
        reference_type: isReceived ? "SALE" : "PURCHASE",
        reference_id: Number(referenceId),
        amount,
        payment_date: paymentDate,
        payment_method: method,
        transaction_reference: txnRef,
        notes,
      });
      onSaved();
    } catch (err) {
      setError(
        err?.response?.data?.message ||
          Object.values(err?.response?.data?.errors || {}).flat()[0] ||
          "Unable to record payment."
      );
    } finally {
      setSaving(false);
    }
  };

  return (
    <Form onSubmit={handleSubmit}>
      {error && <Alert variant="danger" className="py-2">{error}</Alert>}
      <Row>
        <Col md={6} className="mb-3">
          <Form.Group controlId="pay-type">
            <Form.Label>Payment type *</Form.Label>
            <Form.Select
              value={payType}
              onChange={(e) => handleTypeChange(e.target.value)}
            >
              {PAYMENT_TYPES.map((t) => (
                <option key={t} value={t}>
                  {t === "RECEIVED" ? "Received from customer" : "Paid to supplier"}
                </option>
              ))}
            </Form.Select>
          </Form.Group>
        </Col>
        <Col md={6} className="mb-3">
          <Form.Group controlId="pay-date">
            <Form.Label>Date *</Form.Label>
            <Form.Control
              type="date"
              value={paymentDate}
              onChange={(e) => setPaymentDate(e.target.value)}
              required
            />
          </Form.Group>
        </Col>
      </Row>

      <Form.Group className="mb-3" controlId="pay-ref">
        <Form.Label>{isReceived ? "Sale invoice *" : "Purchase bill *"}</Form.Label>
        <Form.Select value={referenceId} onChange={handleReferenceChange} required>
          <option value="">-- Select --</option>
          {references.map((r) => (
            <option key={r.id} value={r.id}>
              {r.invoice_number} — {isReceived ? r.customer_name : r.supplier_name} (due ₹{money(r.due_amount)})
            </option>
          ))}
        </Form.Select>
        {references.length === 0 && (
          <Form.Text className="text-muted">
            No {isReceived ? "sales" : "purchases"} with an outstanding balance in this venture.
          </Form.Text>
        )}
      </Form.Group>

      <Row>
        <Col md={4} className="mb-3">
          <Form.Group controlId="pay-amount">
            <Form.Label>Amount (max ₹{money(maxDue)}) *</Form.Label>
            <Form.Control
              type="number"
              step="0.01"
              min="0.01"
              max={maxDue || undefined}
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              required
            />
          </Form.Group>
        </Col>
        <Col md={4} className="mb-3">
          <Form.Group controlId="pay-method">
            <Form.Label>Payment method *</Form.Label>
            <Form.Select value={method} onChange={(e) => setMethod(e.target.value)}>
              {PAYMENT_METHODS.map((m) => (
                <option key={m} value={m}>{label(m)}</option>
              ))}
            </Form.Select>
          </Form.Group>
        </Col>
        <Col md={4} className="mb-3">
          <Form.Group controlId="pay-txn">
            <Form.Label>Txn reference</Form.Label>
            <Form.Control
              value={txnRef}
              onChange={(e) => setTxnRef(e.target.value)}
              placeholder="UTR / receipt no."
            />
          </Form.Group>
        </Col>
      </Row>

      <Form.Group className="mb-4" controlId="pay-notes">
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
          {saving ? "Recording..." : "Record payment"}
        </Button>
      </div>
    </Form>
  );
}

function PaymentsPage() {
  const { hasPerm } = useAuth();
  const [showForm, setShowForm] = useState(false);
  const [payTypeFilter, setPayTypeFilter] = useState("");

  const { data, loading, error, refetch } = useApi(
    () => fetchPayments({ payment_type: payTypeFilter }),
    [payTypeFilter]
  );
  const payments = useMemo(
    () => (Array.isArray(data?.results) ? data.results : data || []),
    [data]
  );

  const { data: salesData } = useApi(() => fetchSales({ page_size: 100 }), []);
  const sales = useMemo(
    () => (Array.isArray(salesData?.results) ? salesData.results : salesData || []),
    [salesData]
  );
  const { data: purchasesData } = useApi(() => fetchPurchases({ page_size: 100 }), []);
  const purchases = useMemo(
    () =>
      Array.isArray(purchasesData?.results) ? purchasesData.results : purchasesData || [],
    [purchasesData]
  );

  const canManage = hasPerm("payments.manage");

  const handleDelete = async (id) => {
    await deletePayment(id);
    await refetch();
  };

  return (
    <div>
      <div className="d-flex flex-wrap align-items-center justify-content-between mb-4">
        <div>
          <h4 className="mb-1">Payments</h4>
          <p className="text-muted mb-0">Settle sale and purchase balances</p>
        </div>
        {canManage && (
          <Button variant="primary" onClick={() => setShowForm(true)}>
            <i className="bi bi-plus-lg me-2" />
            Record payment
          </Button>
        )}
      </div>

      <Card className="shadow-sm">
        <Card.Header className="bg-white">
          <div className="d-flex flex-wrap align-items-center gap-2">
            <InputGroup style={{ maxWidth: 220 }}>
              <InputGroup.Text><i className="bi bi-filter" /></InputGroup.Text>
              <Form.Select
                value={payTypeFilter}
                onChange={(e) => setPayTypeFilter(e.target.value)}
              >
                <option value="">All types</option>
                {PAYMENT_TYPES.map((t) => (
                  <option key={t} value={t}>{label(t)}</option>
                ))}
              </Form.Select>
            </InputGroup>
            <span className="ms-auto text-muted small align-self-center">
              {data?.count ?? payments.length} payment(s)
            </span>
          </div>
        </Card.Header>
        <Card.Body>
          {loading ? (
            <LoadingSpinner label="Loading payments..." />
          ) : error ? (
            <Alert variant="danger" className="mb-0">Unable to load payments.</Alert>
          ) : (
            <Table hover responsive className="mb-0">
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Type</th>
                  <th>Reference</th>
                  <th>Method</th>
                  <th className="text-end">Amount</th>
                  <th>Txn ref</th>
                  {canManage && <th></th>}
                </tr>
              </thead>
              <tbody>
                {payments.map((p) => (
                  <tr key={p.id}>
                    <td className="text-muted">{p.payment_date}</td>
                    <td>
                      <Badge bg={typeBadge(p.payment_type)}>
                        {label(p.payment_type)}
                      </Badge>
                    </td>
                    <td>
                      <div className="fw-semibold">{p.reference_label}</div>
                      <small className="text-muted">{label(p.reference_type)}</small>
                    </td>
                    <td>{label(p.payment_method)}</td>
                    <td className="text-end fw-semibold">
                      <span className={p.payment_type === "RECEIVED" ? "text-success" : "text-danger"}>
                        {p.payment_type === "RECEIVED" ? "+" : "−"}₹{money(p.amount)}
                      </span>
                    </td>
                    <td className="text-muted">{p.transaction_reference || "—"}</td>
                    {canManage && (
                      <td className="text-end">
                        <Button
                          variant="link"
                          size="sm"
                          className="text-danger p-0"
                          onClick={() => handleDelete(p.id)}
                          title="Reverse payment"
                        >
                          <i className="bi bi-arrow-counterclockwise" />
                        </Button>
                      </td>
                    )}
                  </tr>
                ))}
                {payments.length === 0 && (
                  <tr>
                    <td colSpan={7} className="text-center text-muted py-4">
                      No payments recorded yet.
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
          <Modal.Title>Record payment</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <PaymentForm
            sales={sales}
            purchases={purchases}
            onCancel={() => setShowForm(false)}
            onSaved={async () => {
              await refetch();
              setShowForm(false);
            }}
          />
        </Modal.Body>
      </Modal>
    </div>
  );
}

export default PaymentsPage;