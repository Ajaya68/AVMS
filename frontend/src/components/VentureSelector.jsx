import { Form } from "react-bootstrap";

import { useApi } from "../hooks/useApi";
import { fetchVentures } from "../services/ventureService";
import { useVenture } from "../context/VentureContext";

function VentureSelector() {
  const { data } = useApi(() => fetchVentures({ page_size: 100 }), []);
  const ventures =
    Array.isArray(data?.results) ? data.results : data || [];

  const { ventureId, setVentureId } = useVenture();

  return (
    <Form.Select
      size="sm"
      style={{ maxWidth: 220 }}
      value={ventureId}
      aria-label="Active venture"
      onChange={(e) => setVentureId(e.target.value)}
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