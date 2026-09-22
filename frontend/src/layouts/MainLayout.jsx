import { useState } from 'react';
import { NavLink, useNavigate, Outlet } from 'react-router-dom';
import { useAuth } from '../modules/auth/AuthContext';
import { useTheme } from '../hooks/useUi';

const ICONS = {
  '/': '📊',
  '/organization': '🏢',
  '/users': '👥',
  '/business-units': '🏭',
};

export default function MainLayout() {
  const { user, logout, businessUnits, activeBusinessUnitId, selectBusinessUnit, hasPermission } = useAuth();
  const { theme, toggle } = useTheme();
  const [navOpen, setNavOpen] = useState(false);
  const navigate = useNavigate();

  const nav = [
    { to: '/', label: 'Dashboard', end: true, permission: null, desc: 'Overview & KPIs' },
    { to: '/organization', label: 'Organization', permission: 'ORG.VIEW', desc: 'Company profile' },
    { to: '/users', label: 'Users', permission: 'USER.VIEW', desc: 'Team & access' },
    { to: '/business-units', label: 'Business Units', permission: 'BU.VIEW', desc: 'Mushroom · Fish' },
  ].filter((item) => !item.permission || hasPermission(item.permission));

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  return (
    <div className={`av-shell${navOpen ? ' nav-open' : ''}`}>
      <div className="av-scene" aria-hidden="true">
        <div className="av-blob b1" />
        <div className="av-blob b2" />
        <div className="av-blob b3" />
        <div className="av-grid-overlay" />
      </div>

      <button className="av-scrim" aria-label="Close menu" onClick={() => setNavOpen(false)} />

      <aside className="av-sidebar" aria-label="Primary navigation">
        <div className="av-brand">
          <div className="av-logo">A</div>
          <div>
            <div className="fw-extrabold" style={{ letterSpacing: '-0.02em', lineHeight: 1.1 }}>
              AJAYA <span className="av-gradient-text">VENTURE</span>
            </div>
            <div className="small text-muted" style={{ fontSize: '0.75rem' }}>
              Mushroom · Fish Farming
            </div>
          </div>
        </div>

        <div className="small text-muted px-2 mb-1" style={{ fontWeight: 700, letterSpacing: '.08em', fontSize: '.7rem' }}>
          MENU
        </div>
        <nav className="d-flex flex-column">
          {nav.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              onClick={() => setNavOpen(false)}
              className={({ isActive }) => 'av-nav-link' + (isActive ? ' active' : '')}
            >
              <span className="av-ico" aria-hidden="true">{ICONS[item.to] || '✦'}</span>
              <span>
                <span className="d-block" style={{ lineHeight: 1.15 }}>{item.label}</span>
                <span className="d-block small opacity-75" style={{ fontSize: '.72rem', fontWeight: 500 }}>
                  {item.desc}
                </span>
              </span>
            </NavLink>
          ))}
        </nav>

        <div className="mt-auto">
          <div className="av-glass p-3 d-flex align-items-center gap-2">
            <div
              className="rounded-circle d-grid place-items-center fw-bold text-white flex-shrink-0"
              style={{
                width: 40, height: 40, display: 'grid', placeItems: 'center',
                background: 'linear-gradient(135deg,#6366f1,#06b6d4)',
              }}
            >
              {(user?.username || 'A').slice(0, 1).toUpperCase()}
            </div>
            <div className="min-w-0 flex-grow-1">
              <div className="fw-bold text-truncate" style={{ fontSize: '.88rem' }}>
                {user ? user.username : 'Guest'}
              </div>
              <div className="text-muted text-truncate" style={{ fontSize: '.72rem' }}>
                {user?.role || 'Signed in'}
              </div>
            </div>
            <button
              className="btn btn-sm btn-outline-danger av-btn-press"
              onClick={handleLogout}
              title="Sign out"
            >
              ⎋
            </button>
          </div>
        </div>
      </aside>

      <div className="av-main">
        <header className="av-topbar av-glass">
          <button
            className="btn btn-light av-hamburger av-btn-press"
            onClick={() => setNavOpen((v) => !v)}
            aria-label="Toggle menu"
            style={{ borderRadius: 12 }}
          >
            ☰
          </button>
          <div className="av-hide-sm">
            <div className="fw-extrabold" style={{ lineHeight: 1.1 }}>Welcome back 👋</div>
            <div className="text-muted small">Here&apos;s what&apos;s growing today</div>
          </div>
          <div className="ms-auto d-flex align-items-center gap-2 flex-wrap justify-content-end">
            <select
              className="form-select form-select-sm av-input"
              aria-label="Business unit"
              value={activeBusinessUnitId || ''}
              onChange={(e) => selectBusinessUnit(Number(e.target.value) || null)}
              style={{ maxWidth: 210, borderRadius: 12 }}
            >
              {businessUnits.length > 1 && <option value="">🌐 All Businesses</option>}
              {businessUnits.map((bu) => (
                <option key={bu.business_unit_id} value={bu.business_unit_id}>
                  {bu.business_unit_name}
                </option>
              ))}
            </select>
            <button className="av-theme-btn av-btn-press" onClick={toggle} title="Toggle theme" aria-label="Toggle theme">
              {theme === 'light' ? '🌙' : '☀️'}
            </button>
          </div>
        </header>

        <main className="av-content">
          <div className="av-page" key={window.location.pathname}>
            <Outlet />
          </div>
        </main>
        <footer className="av-footer">
          Ajaya Venture · AB Mushroom Farming · Ajaya Fish Farming · Crafted for growth 🌱
        </footer>
      </div>
    </div>
  );
}
