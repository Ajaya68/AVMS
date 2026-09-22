import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from './AuthContext';
import { ApiError } from '../../services/api';
import PasswordChange from './PasswordChange';

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPw, setShowPw] = useState(false);
  const [showPasswordChange, setShowPasswordChange] = useState(false);
  const [processing, setProcessing] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const submit = async (e) => {
    e.preventDefault();
    setProcessing(true);
    setErrorMessage('');
    try {
      const data = await login(username, password);
      if (data.must_change_password) {
        setShowPasswordChange(true);
        return;
      }
      navigate('/');
    } catch (error) {
      if (error instanceof ApiError) {
        setErrorMessage(error.message || 'Login failed.');
      } else {
        setErrorMessage('Unable to reach the server.');
      }
    } finally {
      setProcessing(false);
    }
  };

  if (showPasswordChange) {
    return <PasswordChange required onDone={() => navigate('/')} />;
  }

  return (
    <div className="av-login-wrap">
      <div className="av-scene" aria-hidden="true">
        <div className="av-blob b1" />
        <div className="av-blob b2" />
        <div className="av-blob b3" />
      </div>

      {/* Left hero — hidden on mobile via CSS */}
      <div className="av-login-hero">
        <div className="av-login-orb" style={{ width: 120, height: 120, right: '12%', top: '14%' }} />
        <div className="av-login-orb" style={{ width: 70, height: 70, right: '28%', top: '38%', animationDelay: '-2s' }} />
        <div className="av-login-orb" style={{ width: 46, height: 46, right: '10%', top: '52%', animationDelay: '-4s' }} />
        <div style={{ position: 'relative', zIndex: 1 }}>
          <div className="d-flex align-items-center gap-2 mb-3">
            <div className="av-logo">A</div>
            <strong>AJAYA VENTURE</strong>
          </div>
          <h1 className="fw-extrabold mb-2" style={{ fontSize: 'clamp(1.8rem,4vw,3rem)', letterSpacing: '-0.03em', lineHeight: 1.05 }}>
            Grow mushrooms.<br />Raise fish.<br />Run it all here.
          </h1>
          <p className="opacity-75 mb-4" style={{ maxWidth: 420 }}>
            AB Mushroom Farming & Ajaya Fish Farming — inventory, production, sales and team in one animated workspace.
          </p>
          <div className="d-flex gap-2">
            {['🍄 Sheds', '🐟 Ponds', '📊 Reports'].map((t) => (
              <span key={t} className="badge rounded-pill" style={{ background: 'rgba(255,255,255,.2)', backdropFilter: 'blur(6px)' }}>{t}</span>
            ))}
          </div>
        </div>
      </div>

      {/* Right form */}
      <div className="av-login-form-side">
        <div className="av-glass av-pop-in p-4 p-md-5 w-100" style={{ maxWidth: 430 }}>
          <div className="text-center mb-1">
            <div className="av-logo mx-auto mb-2">A</div>
            <h1 className="h5 fw-extrabold mb-0">Welcome back 👋</h1>
            <p className="text-muted small">Sign in to your farm workspace</p>
          </div>
          {errorMessage && <div className="alert alert-danger py-2 small av-fade-alert">{errorMessage}</div>}
          <form onSubmit={submit}>
            <div className="mb-3">
              <label className="form-label fw-semibold" htmlFor="username">Username</label>
              <input
                id="username"
                className="form-control av-input"
                style={{ borderRadius: 12, padding: '11px 14px' }}
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                autoComplete="username"
                placeholder="e.g. admin"
                required
              />
            </div>
            <div className="mb-3">
              <label className="form-label fw-semibold" htmlFor="password">Password</label>
              <div className="input-group">
                <input
                  id="password"
                  type={showPw ? 'text' : 'password'}
                  className="form-control av-input"
                  style={{ borderRadius: '12px 0 0 12px', padding: '11px 14px' }}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  autoComplete="current-password"
                  placeholder="••••••••"
                  required
                />
                <button
                  type="button"
                  className="btn btn-outline-secondary"
                  style={{ borderRadius: '0 12px 12px 0' }}
                  onClick={() => setShowPw((v) => !v)}
                  aria-label={showPw ? 'Hide password' : 'Show password'}
                >
                  {showPw ? '🙈' : '👁️'}
                </button>
              </div>
            </div>
            <button className="btn av-gradient-btn w-100 py-2 av-btn-press" disabled={processing}>
              {processing ? (
                <span className="d-inline-flex align-items-center gap-2">
                  <span className="spinner-border spinner-border-sm" /> Signing in…
                </span>
              ) : 'Sign in →'}
            </button>
            <p className="text-center text-muted small mt-3 mb-0">
              🌱 Secure session · business-unit scoped access
            </p>
          </form>
        </div>
      </div>
    </div>
  );
}
