import { useState } from "react";
import { Alert, Form, InputGroup } from "react-bootstrap";
import { Link, useNavigate, useSearchParams } from "react-router-dom";

import { resetPassword } from "../../services/authService";

function ResetPassword() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const uidb64 = searchParams.get("uidb64") || "";
  const token = searchParams.get("token") || "";

  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const invalidLink = !uidb64 || !token;

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");
    if (password !== confirm) {
      setError("Passwords do not match.");
      return;
    }
    setSubmitting(true);
    try {
      await resetPassword(uidb64, token, password);
      navigate("/login", { replace: true, state: { reset: true } });
    } catch (err) {
      setError(
        err?.response?.data?.message || "Unable to reset your password."
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
            <i className="bi bi-key fs-2 text-primary" />
            <div>
              <h4 className="mb-0">Choose a new password</h4>
              <small className="text-muted">
                Your password must be at least 8 characters and not too common
              </small>
            </div>
          </div>

          {error && (
            <div className="alert alert-danger py-2" role="alert">
              {error}
            </div>
          )}

          {invalidLink ? (
            <Alert variant="danger" className="mb-3">
              This password reset link is invalid or incomplete. Request a new
              one from the{" "}
              <Link to="/forgot-password">forgot password page</Link>.
            </Alert>
          ) : (
            <Form onSubmit={handleSubmit}>
              <Form.Group className="mb-3" controlId="reset-password">
                <Form.Label>New password</Form.Label>
                <InputGroup>
                  <Form.Control
                    type={showPassword ? "text" : "password"}
                    placeholder="New password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    required
                    autoFocus
                  />
                  <InputGroup.Text
                    as="button"
                    type="button"
                    className="btn btn-outline-secondary"
                    aria-label={showPassword ? "Hide password" : "Show password"}
                    onClick={() => setShowPassword((v) => !v)}
                  >
                    <i
                      className={`bi ${showPassword ? "bi-eye-slash" : "bi-eye"}`}
                    />
                  </InputGroup.Text>
                </InputGroup>
              </Form.Group>

              <Form.Group className="mb-3" controlId="reset-password-confirm">
                <Form.Label>Confirm new password</Form.Label>
                <Form.Control
                  type={showPassword ? "text" : "password"}
                  placeholder="Confirm new password"
                  value={confirm}
                  onChange={(e) => setConfirm(e.target.value)}
                  required
                />
              </Form.Group>

              <button
                className="btn btn-primary w-100"
                disabled={submitting}
              >
                {submitting ? "Resetting..." : "Reset password"}
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

export default ResetPassword;