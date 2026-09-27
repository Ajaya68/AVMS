import { createContext, useCallback, useContext, useMemo, useState } from "react";

import { getActiveVentureId, setActiveVentureId } from "../services/api";

/**
 * Venture scope shared across the app without a page reload.
 *
 * The selected venture is persisted in localStorage (the API layer reads it
 * for the X-Venture-Id header) and mirrored in React state so dropdowns
 * re-render. Route content is keyed by the venture id (see AppRoutes), so
 * switching ventures remounts the current module and every list refetches
 * under the new scope - no window.location.reload() needed.
 */
const VentureContext = createContext(null);

export function VentureProvider({ children }) {
  const [ventureId, setVentureIdState] = useState(() => getActiveVentureId());

  const setVentureId = useCallback((id) => {
    const normalized = id || "";
    setActiveVentureId(normalized || null);
    setVentureIdState(normalized);
  }, []);

  const value = useMemo(
    () => ({ ventureId, setVentureId }),
    [ventureId, setVentureId]
  );

  return (
    <VentureContext.Provider value={value}>{children}</VentureContext.Provider>
  );
}

// eslint-disable-next-line react-refresh/only-export-components
export function useVenture() {
  return useContext(VentureContext);
}
