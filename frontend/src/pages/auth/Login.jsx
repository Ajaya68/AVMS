import { Link } from "react-router-dom";

function Login() {
  return (
    <div className="auth-shell d-flex align-items-center justify-content-center">
      <div className="card shadow-sm auth-card">
        <div className="card-body p-4">
          <div className="d-flex align-items-center gap-2 mb-3">
            <i className="bi bi-asterisk fs-2 text-primary" />
            <div>
              <h4 className="mb-0">AVMS</h4>
              <small className="text-muted">Sign in to continue</small>
            </div>
          </div>
          <div className="alert alert-info">
            Authentication is implemented in <strong>Phase 2</strong>.
            Use the Demo link below meanwhile.
          </div>
          <Link to="/" className="btn btn-primary w-100">
            Continue to Dashboard
          </Link>
        </div>
      </div>
    </div>
  );
}

export default Login;