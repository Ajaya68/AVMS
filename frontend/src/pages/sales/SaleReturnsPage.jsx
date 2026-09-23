import { useMemo, useState } from "react";
import {
  Alert,
  Badge,
  Button,
  Card,
  Form,
  InputGroup,
  Modal,
  Table,
} from "react-bootstrap";

import LoadingSpinner from "../../components/LoadingSpinner";
import { useAuth } from "../../context/AuthContext";
import { useApi } from "../../hooks/useApi";
import { fetchSaleReturns } from "../../services/saleService";
import SaleReturnForm from "./SaleReturnForm";

function money(n) {
  return Number(n || 0).toFixed(2);
}

function ReturnDetail({ record }) {
  if (!record) return null;
  return (
    <div>
      <div className="d-flex justify-content-between align-items-start mb-3">
        <div>
          <h5 className="mb-1">{record.customer_name}</h5>
          <span className="text-muted">{record.sale_invoice}</span>
        </div>
        <div className="text-end">
          <div className="fw-bold">{record.return_number}</div>
          <span className="text-muted small">{record.return_date}</span>
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
          {(record.items || []).map((it) => (
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
      <div className="text-end">
        <span className="text-muted me-2">Return total</span>
        <span className="fs-5 fw-bold">₹{money(record.total_amount)}</span>
      </div>
    </div>
  );
}

function SaleReturnsPage() {
  const { hasPerm } = useAuth();
  const [showForm, setShowForm] = useState(false);
  const [selected, setSelected] = useState(null);
  const [search, setSearch] = useState("");

  const { data, loading, error, refetch } = useApi(
    () => fetchSaleReturns({ search }),
    [search]
  );
  const records = useMemo(
    () => (Array.isArray(data?.results) ? data.results : data || []),
    [data]
  );

  const canManage = hasPerm("sales_returns.manage");

  return (
    <div>
      <div className="d-flex flex-wrap align-items-center justify-content-between mb-4">
        <div>
          <h4 className="mb-1">Sales Returns</h4>
          <p className="text-muted mb-0">Goods taken back from customers, stock restored</p>
        </div>
        {canManage && (
          <Button variant="primary" onClick={() => setShowForm(true)}>
            <i className="bi bi-plus-lg me-2" />
            New return
          </Button>
        )}
      </div>

      <Card className="shadow-sm">
        <Card.Header className="bg-white">
          <div className="d-flex flex-wrap gap-2">
            <InputGroup style={{ maxWidth: 280 }}>
              <InputGroup.Text><i className="bi bi-search" /></InputGroup.Text>
              <Form.Control
                placeholder="Search return / invoice"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </InputGroup>
            <span className="ms-auto text-muted small align-self-center">
              {data?.count ?? records.length} return(s)
            </span>
          </div>
        </Card.Header>
        <Card.Body>
          {loading ? (
            <LoadingSpinner label="Loading returns..." />
          ) : error ? (
            <Alert variant="danger" className="mb-0">Unable to load returns.</Alert>
          ) : (
            <Table hover responsive className="mb-0">
              <thead>
                <tr>
                  <th>Return</th>
                  <th>Invoice</th>
                  <th>Customer</th>
                  <th>Date</th>
                  <th className="text-end">Amount</th>
                </tr>
              </thead>
              <tbody>
                {records.map((r) => (
                  <tr key={r.id} onClick={() => setSelected(r)} className="cursor-pointer">
                    <td className="fw-semibold">{r.return_number}</td>
                    <td className="text-muted">{r.sale_invoice}</td>
                    <td>{r.customer_name}</td>
                    <td className="text-muted">{r.return_date}</td>
                    <td className="text-end">
                      <Badge bg="warning" text="dark">₹{money(r.total_amount)}</Badge>
                    </td>
                  </tr>
                ))}
                {records.length === 0 && (
                  <tr>
                    <td colSpan={5} className="text-center text-muted py-4">
                      No sales returns recorded yet.
                    </td>
                  </tr>
                )}
              </tbody>
            </Table>
          )}
        </Card.Body>
      </Card>

      <Modal show={Boolean(selected)} onHide={() => setSelected(null)} size="lg">
        <Modal.Header closeButton>
          <Modal.Title>Return detail</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <ReturnDetail record={selected} />
        </Modal.Body>
      </Modal>

      <Modal show={showForm} onHide={() => setShowForm(false)} size="lg">
        <Modal.Header closeButton>
          <Modal.Title>New sales return</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <SaleReturnForm
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

export default SaleReturnsPage;