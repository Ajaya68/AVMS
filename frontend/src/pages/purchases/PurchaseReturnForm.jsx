import { useEffect, useMemo, useState } from "react";
import { Alert, Button, Col, Form, Row, Table } from "react-bootstrap";

import { fetchProducts } from "../../services/productService";
import {
  createPurchaseReturn,
  fetchPurchase,
  fetchPurchases,
} from "../../services/purchaseService";
import { getActiveVentureId } from "../../services/api";
import { useApi } from "../../hooks/useApi";

function money(n) {
  return Number(n || 0).toFixed(2);
}

function PurchaseReturnForm({ preselectPurchaseId, onCancel, onSaved }) {
  const [purchaseId, setPurchaseId] = useState(preselectPurchaseId || "");
  const [loadingPurchase, setLoadingPurchase] = useState(false);
  const [returnDate, setReturnDate] = useState(new Date().toISOString().slice(0, 10));
  const [lines, setLines] = useState([]);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  const { data: purchasesData } = useApi(() => fetchPurchases({ page_size: 100 }), []);
  const purchases = useMemo(
    () => (Array.isArray(purchasesData?.results) ? purchasesData.results : purchasesData || []),
    [purchasesData]
  );
  const { data: productsData } = useApi(() => fetchProducts({ page_size: 100 }), []);
  const products = useMemo(
    () => (Array.isArray(productsData?.results) ? productsData.results : productsData || []),
    [productsData]
  );

  useEffect(() => {
    if (preselectPurchaseId) {
      setPurchaseId(String(preselectPurchaseId));
      loadPurchase(preselectPurchaseId);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const loadPurchase = async (id) => {
    if (!id) return;
    setLoadingPurchase(true);
    setError("");
    try {
      const resp = await fetchPurchase(id);
      const purchase = resp.data.data;
      setLines(
        purchase.items.map((it) => ({
          product: it.product,
          product_name: it.product_name,
          product_sku: it.product_sku,
          quantity: String(it.quantity),
          unit_price: it.unit_price,
        }))
      );
    } catch {
      setError("Unable to load purchase items.");
    } finally {
      setLoadingPurchase(false);
    }
  };

  const handlePurchaseChange = async (e) => {
    const id = e.target.value;
    setPurchaseId(id);
    if (id) await loadPurchase(id);
    else setLines([]);
  };

  const updateLine = (index, patch) => {
    setLines((prev) =>
      prev.map((l, i) => (i === index ? { ...l, ...patch } : l))
    );
  };

  const total = lines.reduce((sum, l) => {
    const qty = Number(l.quantity || 0);
    const price = Number(l.unit_price || 0);
    return sum + qty * price;
  }, 0);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!purchaseId) {
      setError("Select a purchase to return.");
      return;
    }
    const items = lines
      .filter((l) => Number(l.quantity) > 0)
      .map((l) => ({
        product: Number(l.product),
        quantity: l.quantity,
        unit_price: l.unit_price,
      }));
    if (items.length === 0) {
      setError("Add at least one return line with a quantity.");
      return;
    }
    setSaving(true);
    setError("");
    try {
      await createPurchaseReturn({
        venture: Number(getActiveVentureId()),
        purchase: Number(purchaseId),
        return_date: returnDate,
        items,
      });
      onSaved();
    } catch (err) {
      setError(
        err?.response?.data?.message || "Unable to record purchase return."
      );
    } finally {
      setSaving(false);
    }
  };

  const productName = (id) =>
    products.find((p) => p.id === Number(id))?.product_name || "";

  return (
    <Form onSubmit={handleSubmit}>
      {error && <Alert variant="danger" className="py-2">{error}</Alert>}

      <Row>
        <Col md={7} className="mb-3">
          <Form.Group controlId="ret-purchase">
            <Form.Label>Purchase *</Form.Label>
            <Form.Select
              value={purchaseId}
              onChange={handlePurchaseChange}
              disabled={Boolean(preselectPurchaseId)}
              required
            >
              {!preselectPurchaseId && <option value="">-- Select --</option>}
              {purchases
                .filter((p) => p.status === "COMPLETED" || p.status === "PARTIAL")
                .map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.invoice_number} — {p.supplier_name} (due {money(p.due_amount)})
                  </option>
                ))}
            </Form.Select>
          </Form.Group>
        </Col>
        <Col md={5} className="mb-3">
          <Form.Group controlId="ret-date">
            <Form.Label>Return date *</Form.Label>
            <Form.Control
              type="date"
              value={returnDate}
              onChange={(e) => setReturnDate(e.target.value)}
              required
            />
          </Form.Group>
        </Col>
      </Row>

      {loadingPurchase ? (
        <p className="text-muted">Loading purchase items...</p>
      ) : (
        <Table hover responsive className="mt-2 align-middle">
          <thead>
            <tr>
              <th>Product</th>
              <th style={{ width: 130 }}>Unit price</th>
              <th style={{ width: 130 }}>Return qty</th>
              <th className="text-end">Amount</th>
            </tr>
          </thead>
          <tbody>
            {lines.map((l, i) => (
              <tr key={i}>
                <td>
                  <div className="fw-semibold">{l.product_sku} - {l.product_name}</div>
                  <small className="text-muted">{productName(l.product)}</small>
                </td>
                <td>
                  <Form.Control
                    type="number"
                    step="0.01"
                    value={l.unit_price}
                    onChange={(e) => updateLine(i, { unit_price: e.target.value })}
                  />
                </td>
                <td>
                  <Form.Control
                    type="number"
                    step="0.01"
                    min="0"
                    value={l.quantity}
                    onChange={(e) => updateLine(i, { quantity: e.target.value })}
                  />
                </td>
                <td className="text-end fw-semibold">
                  {money(Number(l.quantity || 0) * Number(l.unit_price || 0))}
                </td>
              </tr>
            ))}
            {lines.length === 0 && (
              <tr>
                <td colSpan={4} className="text-center text-muted py-3">
                  Select a purchase to load its items.
                </td>
              </tr>
            )}
          </tbody>
          <tfoot>
            <tr>
              <th colSpan={3} className="text-end">Return total</th>
              <th className="text-end">₹{money(total)}</th>
            </tr>
          </tfoot>
        </Table>
      )}

      <div className="d-flex justify-content-end gap-2">
        <Button variant="secondary" onClick={onCancel}>Cancel</Button>
        <Button type="submit" variant="primary" disabled={saving || loadingPurchase}>
          {saving ? "Recording..." : "Record return"}
        </Button>
      </div>
    </Form>
  );
}

export default PurchaseReturnForm;