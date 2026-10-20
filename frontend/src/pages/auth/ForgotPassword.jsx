import { useState } from "react";
import { Alert, Form } from "react-bootstrap";
import { Link } from "react-router-dom";

import { requestPasswordReset } from "../../services/authService";

function ForgotPassword() {
  const [email, setEmail] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [sentTo, setSentTo] = useState("");
  const [error, setError] = useState("");

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");
    setSubmitting(true);
    try {
      await requestPasswordReset(email);
      setSentTo(email.trim());
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
                A one-time code (OTP) will be emailed to you
              </small>
            </div>
          </div>

          {error && (
            <div className="alert alert-danger py-2" role="alert">
              {error}
            </div>
          )}

          {sentTo ? (
            <>
              <Alert variant="success" className="mb-3">
                If an account exists for that email, a one-time reset code has
                been sent to your email address. It is valid for 10 minutes.
              </Alert>
              <Link
                to={`/reset-password?email=${encodeURIComponent(sentTo)}`}
                className="btn btn-outline-primary w-100"
              >
                <i className="bi bi-key me-2" />
                I have the code — reset password
              </Link>
            </>
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
                {submitting ? "Sending..." : "Email me a one-time code"}
              </button>
            </Form>
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
