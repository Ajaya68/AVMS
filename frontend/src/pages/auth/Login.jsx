import { useState } from "react";
import { Form } from "react-bootstrap";
import { useNavigate } from "react-router-dom";

import { useAuth } from "../../context/AuthContext";

function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");
    setSubmitting(true);
    try {
      await login(email, password);
      navigate("/", { replace: true });
    } catch (err) {
      const detail =
        err?.response?.data?.message ||
        err?.response?.data?.detail ||
        "Unable to sign in. Check your credentials and try again.";
      setError(detail);
    } finally {
      setSubmitting(false);
    }
  };

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

          {error && (
            <div className="alert alert-danger py-2" role="alert">
              {error}
            </div>
          )}

          <Form onSubmit={handleSubmit}>
            <Form.Group className="mb-3" controlId="login-email">
              <Form.Label>Email address</Form.Label>
              <Form.Control
                type="email"
                placeholder="you@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                autoFocus
              />
            </Form.Group>

            <Form.Group className="mb-3" controlId="login-password">
              <Form.Label>Password</Form.Label>
              <Form.Control
                type="password"
                placeholder="Password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </Form.Group>

            <button
              type="submit"
              className="btn btn-primary w-100"
              disabled={submitting}
            >
              {submitting ? "Signing in..." : "Sign in"}
            </button>
          </Form>
        </div>
      </div>
    </div>
  );
}

export default Login;