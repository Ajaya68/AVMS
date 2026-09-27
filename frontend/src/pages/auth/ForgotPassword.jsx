import { useState } from "react";
import { Alert, Form } from "react-bootstrap";
import { Link } from "react-router-dom";

import { requestPasswordReset } from "../../services/authService";

function ForgotPassword() {
  const [email, setEmail] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [done, setDone] = useState(false);
  const [devLink, setDevLink] = useState("");
  const [error, setError] = useState("");

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");
    setSubmitting(true);
    try {
      const data = await requestPasswordReset(email);
      setDone(true);
      // In DEBUG mode the backend returns a usable reset link so the flow
      // is demonstrable without an SMTP relay. Production emails it instead.
      if (data?.reset_url) {
        setDevLink(data.reset_url);
      }
    } catch (err) {
      setError(
        err?.response?.data?.message || "Unable to request a password reset."
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="auth-shell d-flex align-items-center justify-content-center">
      <div className="card shadow-sm auth-card">
        <div className="card-body p-4">
          <div className="d-flex align-items-center gap-2 mb-3">
            <i className="bi bi-shield-lock fs-2 text-primary" />
            <div>
              <h4 className="mb-0">Reset password</h4>
              <small className="text-muted">
                We will send you a link to reset your password
              </small>
            </div>
          </div>

          {error && (
            <div className="alert alert-danger py-2" role="alert">
              {error}
            </div>
          )}

          {done ? (
            <Alert variant="success" className="mb-3">
              If an account exists for that email, a password reset link has
              been sent.
            </Alert>
          ) : (
            <Form onSubmit={handleSubmit}>
              <Form.Group className="mb-3" controlId="forgot-email">
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
              <button
                type="submit"
                className="btn btn-primary w-100"
                disabled={submitting}
              >
                {submitting ? "Sending..." : "Send reset link"}
              </button>
            </Form>
          )}

          {devLink && (
            <div className="mt-3 small text-muted border rounded p-2">
              <div className="mb-1">
                <i className="bi bi-info-circle me-1" />
                Development build: your reset link is
              </div>
              <Link to={new URL(devLink).pathname + new URL(devLink).search}>
                Continue to reset password
              </Link>
            </div>
          )}

          <div className="text-center mt-3">
            <Link to="/login" className="small text-decoration-none">
              <i className="bi bi-arrow-left me-1" />
              Back to sign in
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}

export default ForgotPassword;