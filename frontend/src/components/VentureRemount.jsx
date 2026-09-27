import MainLayout from "../layouts/MainLayout";
import { useVenture } from "../context/VentureContext";

/**
 * Remounts the layout (and therefore the active module) whenever the
 * venture scope changes, so every list refetches under the new venture
 * without a full page reload.
 */
function VentureRemount() {
  const { ventureId } = useVenture();
  return <MainLayout key={ventureId || "all"} />;
}

export default VentureRemount;
