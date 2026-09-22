import { Link } from 'react-router-dom';

export default function NotFound() {
  return (
    <div className="av-glass text-center p-5 av-pop-in mx-auto" style={{ maxWidth: 520 }}>
      <div className="av-float" style={{ fontSize: '4.5rem' }}>🧭</div>
      <h1 className="fw-extrabold av-gradient-text" style={{ fontSize: '3.5rem', letterSpacing: '-0.04em' }}>404</h1>
      <p className="text-muted">That trail leads off the farm. The page you requested does not exist.</p>
      <Link to="/" className="btn av-gradient-btn av-btn-press">← Back to dashboard</Link>
    </div>
  );
}
