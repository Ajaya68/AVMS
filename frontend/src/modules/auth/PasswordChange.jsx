import { useState } from 'react';
import { useAuth } from './AuthContext';
import api, { ApiError } from '../../services/api';

export default function PasswordChange({ required, onDone }) {
  const { user } = useAuth();
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [processing, setProcessing] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  const submit = async (e) => {
    e.preventDefault();
    if (newPassword !== confirmPassword) {
      setErrorMessage('New passwords do not match.');
      return;
    }
    setProcessing(true);
    setErrorMessage('');
    try {
      await api.post('/api/v1/auth/change-password', {
        current_password: currentPassword,
        new_password: newPassword,
      });
      setSuccessMessage('Password changed.');
      if (!required) {
        onDone && onDone();
        return;
      }
    } catch (error) {
      if (error instanceof ApiError) {
        const field = error.fieldErrors && error.fieldErrors[0];
        setErrorMessage(field ? field.message : error.message);
      } else {
        setErrorMessage('Unable to reach the server.');
      }
    } finally {
      setProcessing(false);
    }
  };

  const ready = required && successMessage;
  if (ready) {
    return (
      <div className="av-login-form-side vh-100">
        <div className="av-scene" aria-hidden="true"><div className="av-blob b1" /><div className="av-blob b2" /></div>
        <div className="av-glass av-pop-in p-4 text-center w-100" style={{ maxWidth: 360 }}>
          <div style={{ fontSize: '3rem' }}>✅</div>
          <p className="mb-3 text-success fw-bold">{successMessage}</p>
          <button className="btn av-gradient-btn w-100 av-btn-press" onClick={onDone}>Continue →</button>
        </div>
      </div>
    );
  }

  return (
    <div className="av-login-form-side vh-100">
      <div className="av-scene" aria-hidden="true"><div className="av-blob b1" /><div className="av-blob b2" /><div className="av-blob b3" /></div>
      <div className="av-glass av-pop-in p-4 p-md-5 w-100" style={{ maxWidth: 400 }}>
        <h1 className="h5 fw-extrabold mb-1">🔐 Change password</h1>
          <p className="text-muted small mb-3">
            {user ? user.username : ''} · You must set a new password before continuing.
          </p>
          {errorMessage && <div className="alert alert-danger py-2 small av-fade-alert">{errorMessage}</div>}
          {successMessage && <div className="alert alert-success py-2 small av-fade-alert">{successMessage}</div>}
          <form onSubmit={submit}>
            <div className="mb-3">
              <label className="form-label fw-semibold" htmlFor="currentPassword">Current password</label>
              <input
                id="currentPassword"
                type="password"
                className="form-control av-input"
                style={{ borderRadius: 12 }}
                value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)}
                autoComplete="current-password"
                required
              />
            </div>
            <div className="mb-3">
              <label className="form-label fw-semibold" htmlFor="newPassword">New password</label>
              <input
                id="newPassword"
                type="password"
                className="form-control av-input"
                style={{ borderRadius: 12 }}
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                autoComplete="new-password"
                required
              />
            </div>
            <div className="mb-3">
              <label className="form-label fw-semibold" htmlFor="confirmPassword">Confirm new password</label>
              <input
                id="confirmPassword"
                type="password"
                className="form-control av-input"
                style={{ borderRadius: 12 }}
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                autoComplete="new-password"
                required
              />
            </div>
            <button className="btn av-gradient-btn w-100 av-btn-press" disabled={processing}>
              {processing ? 'Saving…' : 'Save new password'}
            </button>
          </form>
        </div>
      </div>
  );
}