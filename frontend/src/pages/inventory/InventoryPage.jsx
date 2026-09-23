import { useMemo, useState } from "react";
import { Alert, Badge, Card, Form, InputGroup, Table } from "react-bootstrap";

import LoadingSpinner from "../../components/LoadingSpinner";
import { useApi } from "../../hooks/useApi";
import { fetchInventory, fetchWarehouses } from "../../services/inventoryService";

function InventoryPage() {
  const [warehouse, setWarehouse] = useState("");
  const [onlyLow, setOnlyLow] = useState(false);

  const params = useMemo(
    () => ({
      warehouse: warehouse || undefined,
      low: onlyLow ? "true" : undefined,
    }),
    [warehouse, onlyLow]
  );

  const { data, loading, error } = useApi(() => fetchInventory(params), [params]);
  const { data: warehousesData } = useApi(() => fetchWarehouses({ page_size: 100 }), []);
  const warehouses = useMemo(
    () => (Array.isArray(warehousesData?.results) ? warehousesData.results : warehousesData || []),
    [warehousesData]
  );

  const rows = Array.isArray(data?.results) ? data.results : data || [];

  return (
    <div>
      <div className="mb-4">
        <h4 className="mb-1">Inventory</h4>
        <p className="text-muted mb-0">Stock on hand across warehouses</p>
      </div>

      <Card className="shadow-sm">
        <Card.Header className="bg-white">
          <div className="d-flex flex-wrap gap-3 align-items-center">
            <Form.Select
              style={{ maxWidth: 240 }}
              value={warehouse}
              onChange={(e) => setWarehouse(e.target.value)}
            >
              <option value="">All warehouses</option>
              {warehouses.map((w) => (
                <option key={w.id} value={String(w.id)}>
                  {w.warehouse_code} - {w.warehouse_name}
                </option>
              ))}
            </Form.Select>
            <InputGroup style={{ maxWidth: 220 }}>
              <InputGroup.Checkbox
                checked={onlyLow}
                onChange={(e) => setOnlyLow(e.target.checked)}
              />
              <InputGroup.Text>Low stock only</InputGroup.Text>
            </InputGroup>
            <span className="text-muted small ms-auto">
              {data?.count ?? rows.length} record(s)
            </span>
          </div>
        </Card.Header>
        <Card.Body>
          {loading ? (
            <LoadingSpinner label="Loading stock levels..." />
          ) : error ? (
            <Alert variant="danger" className="mb-0">Unable to load inventory.</Alert>
          ) : (
            <Table hover responsive className="mb-0">
              <thead>
                <tr>
                  <th>Product</th>
                  <th>SKU</th>
                  <th>Warehouse</th>
                  <th className="text-end">On hand</th>
                  <th className="text-end">Reserved</th>
                  <th className="text-end">Available</th>
                  <th className="text-end">Reorder level</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((row) => (
                  <tr key={row.id}>
                    <td className="fw-semibold">{row.product_name}</td>
                    <td className="text-muted">{row.product_sku}</td>
                    <td>{row.warehouse_name}</td>
                    <td className="text-end">{row.quantity}</td>
                    <td className="text-end">{row.reserved_quantity}</td>
                    <td className="text-end">{row.available}</td>
                    <td className="text-end">{row.reorder_level}</td>
                    <td>
                      {row.is_low ? (
                        <Badge bg="danger">Low stock</Badge>
                      ) : (
                        <Badge bg="success">In stock</Badge>
                      )}
                    </td>
                  </tr>
                ))}
                {rows.length === 0 && (
                  <tr>
                    <td colSpan={8} className="text-center text-muted py-4">
                      No stock records found. Record a stock movement to see inventory.
                    </td>
                  </tr>
                )}
              </tbody>
            </Table>
          )}
        </Card.Body>
      </Card>
    </div>
  );
}

export default InventoryPage;