import { useEffect, useState } from "react";
import { Alert, Button, Card, Col, Form, Row, Tab, Table, Tabs } from "react-bootstrap";

import LoadingSpinner from "../../components/LoadingSpinner";
import {
  fetchFinancialReport,
  fetchInventoryReport,
  fetchPurchasesReport,
  fetchSalesReport,
} from "../../services/reportService";

const money = (n) => Number(n || 0).toLocaleString(undefined, { minimumFractionDigits: 2 });

function QtyCard({ label, value, tone }) {
  return (
    <Card className="text-center h-100">
      <Card.Body>
        <div className={`fs-4 fw-bold ${tone || "text-primary"}`}>{value}</div>
        <div className="text-muted small">{label}</div>
      </Card.Body>
    </Card>
  );
}

function SeriesChart({ series }) {
  const max = Math.max(...series.map((s) => Number(s.amount)), 1);
  return (
    <div className="d-flex align-items-end gap-2 mt-3" style={{ height: 140 }}>
      {series.map((s) => (
        <div key={s.label} className="flex-grow-1 text-center" title={`${s.label}: ${money(s.amount)}`}>
          <div
            className="bg-primary rounded-top mx-auto"
            style={{ height: `${Math.max((Number(s.amount) / max) * 100, 3)}%`, minWidth: 16, maxWidth: 40 }}
          />
          <div className="small text-muted mt-1" style={{ fontSize: "0.65rem" }}>{s.label.slice(5)}</div>
        </div>
      ))}
    </div>
  );
}

function useReport(fetchFn) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [data, setData] = useState(null);
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");

  async function load(params = {}) {
    setLoading(true);
    setError("");
    try {
      // Only send date filters when set: the backend defaults an absent
      // param to the last 30 days, but an empty string would override the
      // default with an invalid date. Venture scope travels via the
      // X-Venture-Id header, not a query param.
      const query = { ...params };
      if (from) query.from = from;
      if (to) query.to = to;
      setData(await fetchFn(query));
    } catch (e) {
      setError(e?.response?.data?.message || "Failed to load report");
    } finally {
      setLoading(false);
    }
  }

  return { loading, error, data, from, to, setFrom, setTo, load };
}

function SalesTab() {
  const { loading, error, data, load, from, to, setFrom, setTo } = useReport(fetchSalesReport);
  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);
  return (
    <ReportBody title="Sales report" loading={loading} error={error} onLoad={load} variant="primary" from={from} to={to} setFrom={setFrom} setTo={setTo}>
      {data && (
        <>
          <div className="d-flex flex-wrap gap-3 mb-3">
            <QtyCard label="Sales count" value={data.summary.count} />
            <QtyCard label="Items sold" value={data.summary.items_sold} />
            <QtyCard label="Gross sales" value={money(data.summary.total_amount)} tone="text-success" />
            <QtyCard label="Returns" value={money(data.summary.returned_amount)} tone="text-danger" />
            <QtyCard label="Net sales" value={money(data.summary.net_amount)} tone="text-primary" />
          </div>
          <ReportTable
            headers={["Product", "Quantity", "Revenue"]}
            rows={data.top_products.map((p) => [p.product_name, p.quantity, money(p.revenue)])}
            empty="No sales in this period"
          />
          {data.series.length > 0 && (
            <Card className="mt-3">
              <Card.Header>Sales by day</Card.Header>
              <Card.Body>
                <SeriesChart series={data.series} />
              </Card.Body>
            </Card>
          )}
        </>
      )}
    </ReportBody>
  );
}

function PurchasesTab() {
  const { loading, error, data, load, from, to, setFrom, setTo } = useReport(fetchPurchasesReport);
  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);
  return (
    <ReportBody title="Purchases report" loading={loading} error={error} onLoad={load} variant="success" from={from} to={to} setFrom={setFrom} setTo={setTo}>
      {data && (
        <>
          <div className="d-flex flex-wrap gap-3 mb-3">
            <QtyCard label="Purchase count" value={data.summary.count} />
            <QtyCard label="Gross purchases" value={money(data.summary.total_amount)} />
            <QtyCard label="Returns" value={money(data.summary.returned_amount)} tone="text-danger" />
            <QtyCard label="Net purchases" value={money(data.summary.net_amount)} tone="text-success" />
          </div>
          <ReportTable
            headers={["Supplier", "Amount", "Invoices"]}
            rows={data.top_suppliers.map((s) => [s.supplier_name, money(s.amount), s.count])}
            empty="No purchases in this period"
          />
          {data.series.length > 0 && (
            <Card className="mt-3">
              <Card.Header>Purchases by day</Card.Header>
              <Card.Body>
                <SeriesChart series={data.series} />
              </Card.Body>
            </Card>
          )}
        </>
      )}
    </ReportBody>
  );
}

function InventoryTab() {
  const { loading, error, data, load, from, to, setFrom, setTo } = useReport(fetchInventoryReport);
  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);
  return (
    <ReportBody title="Inventory report" loading={loading} error={error} onLoad={load} variant="info" from={from} to={to} setFrom={setFrom} setTo={setTo}>
      {data && (
        <>
          <div className="d-flex flex-wrap gap-3 mb-3">
            <QtyCard label="Products in stock" value={data.summary.stock_products} />
            <QtyCard label="Active products" value={data.summary.active_products} />
            <QtyCard label="Total quantity" value={data.summary.total_quantity} />
            <QtyCard label="Stock value" value={money(data.summary.stock_value)} tone="text-success" />
          </div>
          <ReportTable
            headers={["Product", "Warehouse", "Quantity", "Unit cost", "Value"]}
            rows={data.items.map((i) => [i.product_name, i.warehouse, i.quantity, money(i.unit_cost), money(i.valuation)])}
            empty="No stock on record"
          />
          {data.low_stock.length > 0 && (
            <Card className="mt-3 border-danger">
              <Card.Header className="text-danger">Low stock alerts</Card.Header>
              <Card.Body>
                <Table size="sm" striped hover>
                  <thead>
                    <tr><th>Product</th><th>Warehouse</th><th>Available</th><th>Reorder level</th></tr>
                  </thead>
                  <tbody>
                    {data.low_stock.map((i) => (
                      <tr key={`${i.product_code}-${i.warehouse}`}>
                        <td>{i.product_name}</td>
                        <td>{i.warehouse}</td>
                        <td>{i.quantity}</td>
                        <td>{i.reorder_level}</td>
                      </tr>
                    ))}
                  </tbody>
                </Table>
              </Card.Body>
            </Card>
          )}
        </>
      )}
    </ReportBody>
  );
}

function FinancialTab() {
  const { loading, error, data, load, from, to, setFrom, setTo } = useReport(fetchFinancialReport);
  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);
  return (
    <ReportBody title="Financial report" loading={loading} error={error} onLoad={load} variant="success" from={from} to={to} setFrom={setFrom} setTo={setTo}>
      {data && (
        <>
          <div className="d-flex flex-wrap gap-3 mb-3">
            <QtyCard label="Revenue" value={money(data.summary.revenue)} tone="text-success" />
            <QtyCard label="COGS" value={money(data.summary.cogs)} />
            <QtyCard label="Expenses" value={money(data.summary.expenses)} tone="text-danger" />
            <QtyCard label="Gross margin" value={money(data.summary.gross_margin)} tone="text-primary" />
            <QtyCard label="Net profit" value={money(data.summary.net_profit)} tone={Number(data.summary.net_profit) >= 0 ? "text-success" : "text-danger"} />
          </div>
          <Row className="g-3 mb-3">
            <Col md={6}>
              <Card>
                <Card.Header>Outstanding</Card.Header>
                <Card.Body>
                  <div className="d-flex justify-content-between mb-2">
                    <span>Receivables (customers owe)</span>
                    <span className="fw-bold text-danger">{money(data.outstanding.receivables)}</span>
                  </div>
                  <div className="d-flex justify-content-between">
                    <span>Payables (you owe suppliers)</span>
                    <span className="fw-bold text-warning">{money(data.outstanding.payables)}</span>
                  </div>
                </Card.Body>
              </Card>
            </Col>
            <Col md={6}>
              <Card>
                <Card.Header>Cash flow</Card.Header>
                <Card.Body>
                  <div className="d-flex justify-content-between mb-2">
                    <span>Received</span>
                    <span className="fw-bold text-success">{money(data.cash_flow.received)}</span>
                  </div>
                  <div className="d-flex justify-content-between mb-2">
                    <span>Paid out</span>
                    <span className="fw-bold text-danger">{money(data.cash_flow.paid)}</span>
                  </div>
                  <div className="d-flex justify-content-between">
                    <span>Net</span>
                    <span className="fw-bold">{money(data.cash_flow.net)}</span>
                  </div>
                </Card.Body>
              </Card>
            </Col>
          </Row>
        </>
      )}
    </ReportBody>
  );
}

function ReportBody({ title, loading, error, onLoad, variant, from, to, setFrom, setTo, children }) {
  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h5 className="mb-0">{title}</h5>
        <div className="d-flex gap-2 align-items-center">
          <Form.Control type="date" size="sm" value={from} onChange={(e) => setFrom && setFrom(e.target.value)} />
          <Form.Control type="date" size="sm" value={to} onChange={(e) => setTo && setTo(e.target.value)} />
          <Button size="sm" variant={variant} disabled={loading} onClick={() => onLoad()}>
            {loading ? "Loading..." : "Refresh"}
          </Button>
        </div>
      </div>
      {error && <Alert variant="danger">{error}</Alert>}
      {loading && <LoadingSpinner />}
      {!loading && !error && children}
    </div>
  );
}

function ReportTable({ headers, rows, empty }) {
  if (rows.length === 0) {
    return <Alert variant="light" className="text-muted">{empty}</Alert>;
  }
  return (
    <Card>
      <Card.Body className="p-0">
        <Table size="sm" striped hover className="mb-0">
          <thead>
            <tr>{headers.map((h) => <th key={h}>{h}</th>)}</tr>
          </thead>
          <tbody>
            {rows.map((r, i) => (
              <tr key={i}>{r.map((c, j) => <td key={j}>{c}</td>)}</tr>
            ))}
          </tbody>
        </Table>
      </Card.Body>
    </Card>
  );
}

export default function ReportsPage() {
  const [key, setKey] = useState("sales");
  return (
    <Card>
      <Card.Header>Reports</Card.Header>
      <Card.Body>
        <Tabs activeKey={key} onSelect={setKey} className="mb-3">
          <Tab eventKey="sales" title="Sales">
            <SalesTab />
          </Tab>
          <Tab eventKey="purchases" title="Purchases">
            <PurchasesTab />
          </Tab>
          <Tab eventKey="inventory" title="Inventory">
            <InventoryTab />
          </Tab>
          <Tab eventKey="financial" title="Financial">
            <FinancialTab />
          </Tab>
        </Tabs>
      </Card.Body>
    </Card>
  );
}