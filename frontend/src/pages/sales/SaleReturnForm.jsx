import { useEffect, useMemo, useState } from "react";
import { Alert, Button, Col, Form, Row, Table } from "react-bootstrap";

import { useApi } from "../../hooks/useApi";
import { getActiveVentureId } from "../../services/api";
import { fetchProducts } from "../../services/productService";
import {
  createSaleReturn,
  fetchSale,
  fetchSales,
} from "../../services/saleService";

function money(n) {
  return Number(n || 0).toFixed(2);
}

function SaleReturnForm({ preselectSaleId, onCancel, onSaved }) {
  const [saleId, setSaleId] = useState(preselectSaleId || "");
  const [loadingSale, setLoadingSale] = useState(false);
  const [returnDate, setReturnDate] = useState(new Date().toISOString().slice(0, 10));
  const [lines, setLines] = useState([]);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  const { data: salesData } = useApi(() => fetchSales({ page_size: 100 }), []);
  const sales = useMemo(
    () => (Array.isArray(salesData?.results) ? salesData.results : salesData || []),
    [salesData]
  );
  const { data: productsData } = useApi(() => fetchProducts({ page_size: 100 }), []);
  const products = useMemo(
    () => (Array.isArray(productsData?.results) ? productsData.results : productsData || []),
    [productsData]
  );

  const loadSale = async (id) => {
    if (!id) return;
    setLoadingSale(true);
    setError("");
    try {
      const resp = await fetchSale(id);
      const sale = resp.data.data;
      setLines(
        sale.items.map((it) => ({
          product: it.product,
          product_name: it.product_name,
          product_sku: it.product_sku,
          quantity: String(it.quantity),
          unit_price: it.unit_price,
        }))
      );
    } catch {
      setError("Unable to load sale items.");
    } finally {
      setLoadingSale(false);
    }
  };

  useEffect(() => {
    if (preselectSaleId) {
      setSaleId(String(preselectSaleId));
      loadSale(preselectSaleId);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleSaleChange = async (e) => {
    const id = e.target.value;
    setSaleId(id);
    if (id) await loadSale(id);
    else setLines([]);
  };

  const updateLine = (index, patch) => {
    setLines((prev) =>
      prev.map((l, i) => (i === index ? { ...l, ...patch } : l))
    );
  };

  const total = lines.reduce((sum, l) => {
    return sum + Number(l.quantity || 0) * Number(l.unit_price || 0);
  }, 0);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!saleId) {
      setError("Select a sale to return.");
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
      await createSaleReturn({
        venture: Number(getActiveVentureId()),
        sale: Number(saleId),
        return_date: returnDate,
        items,
      });
      onSaved();
    } catch (err) {
      setError(err?.response?.data?.message || "Unable to record sale return.");
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
          <Form.Group controlId="ret-sale">
            <Form.Label>Sale invoice *</Form.Label>
            <Form.Select
              value={saleId}
              onChange={handleSaleChange}
              disabled={Boolean(preselectSaleId)}
              required
            >
              {!preselectSaleId && <option value="">-- Select --</option>}
              {sales
                .filter((s) => s.status === "COMPLETED" || s.status === "PARTIAL")
                .map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.invoice_number} — {s.customer_name} (due {money(s.due_amount)})
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

      {loadingSale ? (
        <p className="text-muted">Loading sale items...</p>
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
                  Select a sale to load its items.
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
        <Button type="submit" variant="primary" disabled={saving || loadingSale}>
          {saving ? "Recording..." : "Record return"}
        </Button>
      </div>
    </Form>
  );
}

export default SaleReturnForm;