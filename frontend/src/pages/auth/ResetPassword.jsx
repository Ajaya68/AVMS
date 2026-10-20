import { useState } from "react";
import { Alert, Form, InputGroup } from "react-bootstrap";
import { Link, useNavigate, useSearchParams } from "react-router-dom";

import { resetPassword } from "../../services/authService";

function ResetPassword() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const emailParam = searchParams.get("email") || "";

  const [email, setEmail] = useState(emailParam);
  const [otp, setOtp] = useState("");
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");
    if (password !== confirm) {
      setError("Passwords do not match.");
      return;
    }
    setSubmitting(true);
    try {
      await resetPassword(email, otp, password);
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
              <h4 className="mb-0">Reset password with OTP</h4>
              <small className="text-muted">
                Enter the one-time code printed on the server console (valid 10
                minutes)
              </small>
            </div>
          </div>

          {error && (
            <div className="alert alert-danger py-2" role="alert">
              {error}
            </div>
          )}

          <Form onSubmit={handleSubmit}>
            <Form.Group className="mb-3" controlId="reset-email">
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

            <Form.Group className="mb-3" controlId="reset-otp">
              <Form.Label>One-time code (OTP)</Form.Label>
              <Form.Control
                type="text"
                inputMode="numeric"
                pattern="[0-9]{6}"
                maxLength={6}
                placeholder="6-digit code"
                value={otp}
                onChange={(e) => setOtp(e.target.value.replace(/\D/g, ""))}
                required
              />
            </Form.Group>

            <Form.Group className="mb-3" controlId="reset-password">
              <Form.Label>New password</Form.Label>
              <InputGroup>
                <Form.Control
                  type={showPassword ? "text" : "password"}
                  placeholder="New password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                  minLength={8}
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

          <div className="text-center mt-3">
            <Link to="/forgot-password" className="small text-decoration-none">
              <i className="bi bi-arrow-repeat me-1" />
              Request a new code
            </Link>
            <span className="mx-2 text-muted">|</span>
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
