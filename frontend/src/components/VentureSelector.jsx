import { Form } from "react-bootstrap";

import { useApi } from "../hooks/useApi";
import { fetchVentures } from "../services/ventureService";
import { getActiveVentureId, setActiveVentureId } from "../services/api";

function VentureSelector() {
  const { data } = useApi(() => fetchVentures({ page_size: 100 }), []);
  const ventures =
    Array.isArray(data?.results) ? data.results : data || [];

  const active = getActiveVentureId();

  return (
    <Form.Select
      size="sm"
      style={{ maxWidth: 220 }}
      value={active}
      aria-label="Active venture"
      onChange={(e) => setActiveVentureId(e.target.value || null)}
    >
      <option value="">All ventures</option>
      {ventures.map((v) => (
        <option key={v.id} value={String(v.id)}>
          {v.venture_code} - {v.venture_name}
        </option>
      ))}
    </Form.Select>
  );
}

export default VentureSelector;