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

import LoadingSpinner from "../../components/LoadingSpinner";
import { useAuth } from "../../context/AuthContext";
import { useApi } from "../../hooks/useApi";
import { getActiveVentureId } from "../../services/api";
import {
  PAYMENT_METHODS,
  label as methodLabel,
} from "../../services/paymentService";
import {
  createExpense,
  deleteExpense,
  fetchExpenseCategories,
  fetchExpenses,
} from "../../services/expenseService";

function money(n) {
  return Number(n || 0).toFixed(2);
}

function ExpenseForm({ categories, onCancel, onSaved }) {
  const [category, setCategory] = useState("");
  const [amount, setAmount] = useState("");
  const [expenseDate, setExpenseDate] = useState(new Date().toISOString().slice(0, 10));
  const [method, setMethod] = useState("CASH");
  const [description, setDescription] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError("");
    try {
      await createExpense({
        venture: Number(getActiveVentureId()),
        category: Number(category),
        amount,
        expense_date: expenseDate,
        payment_method: method,
        description,
      });
      onSaved();
    } catch (err) {
      setError(
        Object.values(err?.response?.data?.errors || {}).flat()[0] ||
          "Unable to record expense."
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
          <Form.Group controlId="exp-category">
            <Form.Label>Category *</Form.Label>
            <Form.Select value={category} onChange={(e) => setCategory(e.target.value)} required>
              <option value="">-- Select --</option>
              {categories.map((c) => (
                <option key={c.id} value={c.id}>{c.category_name}</option>
              ))}
            </Form.Select>
          </Form.Group>
        </Col>
        <Col md={6} className="mb-3">
          <Form.Group controlId="exp-date">
            <Form.Label>Expense date *</Form.Label>
            <Form.Control
              type="date"
              value={expenseDate}
              onChange={(e) => setExpenseDate(e.target.value)}
              required
            />
          </Form.Group>
        </Col>
      </Row>
      <Row>
        <Col md={6} className="mb-3">
          <Form.Group controlId="exp-amount">
            <Form.Label>Amount (₹) *</Form.Label>
            <Form.Control
              type="number"
              step="0.01"
              min="0.01"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              required
            />
          </Form.Group>
        </Col>
        <Col md={6} className="mb-3">
          <Form.Group controlId="exp-method">
            <Form.Label>Payment method *</Form.Label>
            <Form.Select value={method} onChange={(e) => setMethod(e.target.value)}>
              {PAYMENT_METHODS.map((m) => (
                <option key={m} value={m}>{methodLabel(m)}</option>
              ))}
            </Form.Select>
          </Form.Group>
        </Col>
      </Row>
      <Form.Group className="mb-4" controlId="exp-desc">
        <Form.Label>Description</Form.Label>
        <Form.Control
          as="textarea"
          rows={2}
          value={description}
          onChange={(e) => setDescription(e.target.value)}
        />
      </Form.Group>
      <div className="d-flex justify-content-end gap-2">
        <Button variant="secondary" onClick={onCancel}>Cancel</Button>
        <Button type="submit" variant="primary" disabled={saving}>
          {saving ? "Saving..." : "Save expense"}
        </Button>
      </div>
    </Form>
  );
}

function ExpensesPage() {
  const { hasPerm } = useAuth();
  const [showForm, setShowForm] = useState(false);
  const [search, setSearch] = useState("");
  const [categoryFilter, setCategoryFilter] = useState("");

  const { data, loading, error, refetch } = useApi(
    () => fetchExpenses({ search, category: categoryFilter }),
    [search, categoryFilter]
  );
  const expenses = useMemo(
    () => (Array.isArray(data?.results) ? data.results : data || []),
    [data]
  );
  const { data: categoriesData } = useApi(() => fetchExpenseCategories(), []);
  const categories = useMemo(
    () => (Array.isArray(categoriesData) ? categoriesData : categoriesData || []),
    [categoriesData]
  );

  const canManage = hasPerm("expenses.manage");

  const total = expenses.reduce((s, e) => s + Number(e.amount || 0), 0);

  const handleDelete = async (id) => {
    await deleteExpense(id);
    await refetch();
  };

  return (
    <div>
      <div className="d-flex flex-wrap align-items-center justify-content-between mb-4">
        <div>
          <h4 className="mb-1">Expenses</h4>
          <p className="text-muted mb-0">Operating costs per venture</p>
        </div>
        {canManage && (
          <Button variant="primary" onClick={() => setShowForm(true)}>
            <i className="bi bi-plus-lg me-2" />
            New expense
          </Button>
        )}
      </div>

      <Card className="shadow-sm">
        <Card.Header className="bg-white">
          <div className="d-flex flex-wrap gap-2">
            <InputGroup style={{ maxWidth: 280 }}>
              <InputGroup.Text><i className="bi bi-search" /></InputGroup.Text>
              <Form.Control
                placeholder="Search description / category"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </InputGroup>
            <Form.Select
              style={{ maxWidth: 200 }}
              value={categoryFilter}
              onChange={(e) => setCategoryFilter(e.target.value)}
            >
              <option value="">All categories</option>
              {categories.map((c) => (
                <option key={c.id} value={String(c.id)}>{c.category_name}</option>
              ))}
            </Form.Select>
            <span className="ms-auto text-muted small align-self-center">
              {data?.count ?? expenses.length} expense(s) · ₹{money(total)} shown
            </span>
          </div>
        </Card.Header>
        <Card.Body>
          {loading ? (
            <LoadingSpinner label="Loading expenses..." />
          ) : error ? (
            <Alert variant="danger" className="mb-0">Unable to load expenses.</Alert>
          ) : (
            <Table hover responsive className="mb-0">
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Category</th>
                  <th>Description</th>
                  <th>Method</th>
                  <th className="text-end">Amount</th>
                  {canManage && <th></th>}
                </tr>
              </thead>
              <tbody>
                {expenses.map((e) => (
                  <tr key={e.id}>
                    <td className="text-muted">{e.expense_date}</td>
                    <td>
                      <span className="badge text-bg-light border">{e.category_name}</span>
                    </td>
                    <td>{e.description || "—"}</td>
                    <td className="text-muted">{methodLabel(e.payment_method)}</td>
                    <td className="text-end fw-semibold text-danger">₹{money(e.amount)}</td>
                    {canManage && (
                      <td className="text-end">
                        <Button
                          variant="link"
                          size="sm"
                          className="text-danger p-0"
                          onClick={() => handleDelete(e.id)}
                        >
                          <i className="bi bi-trash" />
                        </Button>
                      </td>
                    )}
                  </tr>
                ))}
                {expenses.length === 0 && (
                  <tr>
                    <td colSpan={6} className="text-center text-muted py-4">
                      No expenses recorded yet.
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
          <Modal.Title>New expense</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <ExpenseForm
            categories={categories}
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

export default ExpensesPage;