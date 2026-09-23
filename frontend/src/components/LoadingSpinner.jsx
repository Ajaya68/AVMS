import Spinner from "react-bootstrap/Spinner";

function LoadingSpinner({ label = "Loading..." }) {
  return (
    <div className="d-flex flex-column align-items-center justify-content-center py-5 text-muted">
      <Spinner animation="border" variant="primary" role="status" />
      <span className="mt-2">{label}</span>
    </div>
  );
}

export default LoadingSpinner;