import { useLocation } from "react-router-dom";

/**
 * Rendering target for modules that are not implemented yet. Honest about
 * the module's status instead of showing a fake, non-functional page.
 */
function PlaceholderPage() {
  const location = useLocation();
  const label = location.pathname
    .split("/")
    .filter(Boolean)
    .map((s) => s.charAt(0).toUpperCase() + s.slice(1).replace(/-/g, " "))
    .join(" / ");

  return (
    <div>
      <h4 className="mb-3">{label || "Module"}</h4>
      <div className="border rounded-3 text-center p-5 bg-light">
        <i className="bi bi-cone-striped fs-1 text-warning" />
        <h5 className="mt-3">Coming soon</h5>
        <p className="text-muted mb-0">
          The <strong>{label}</strong> module will be activated in its
          development phase.
        </p>
      </div>
    </div>
  );
}

export default PlaceholderPage;