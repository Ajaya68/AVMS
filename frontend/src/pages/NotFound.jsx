import { Link } from "react-router-dom";

function NotFound() {
  return (
    <div className="text-center py-5">
      <h1 className="display-1 fw-bold text-muted">404</h1>
      <p className="text-muted mb-3">The page you are looking for does not exist.</p>
      <Link to="/" className="btn btn-primary">
        Back to Dashboard
      </Link>
    </div>
  );
}

export default NotFound;