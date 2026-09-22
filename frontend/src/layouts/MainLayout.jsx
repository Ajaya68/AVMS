import React, { useMemo, useState } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../modules/auth/AuthContext';

// Business-unit context: the selector sends the chosen scope on every API
// request. Backend authorization always re-verifies this value server-side.
export default function MainLayout({ children }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [businessUnitId, setBusinessUnitId] = useState('');

  const nav = [
    { to: '/', label: 'Dashboard', end: true },
  ];

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const businessUnits = useMemo(
    () => (user && user.business_units ? user.business_units : []),
    [user],
  );

  return (
    <div className="d-flex flex-column vh-100">
      <nav className="navbar navbar-expand-lg navbar-dark bg-dark px-3">
        <span className="navbar-brand mb-0 h1">AJAYA VENTURE</span>
        <div className="collapse navbar-collapse" id="avNav">
          <ul className="navbar-nav me-auto">
            {nav.map((item) => (
              <li className="nav-item" key={item.to}>
                <NavLink
                  className={({ isActive }) => 'nav-link' + (isActive ? ' active' : '')}
                  end={item.end}
                  to={item.to}
                >
                  {item.label}
                </NavLink>
              </li>
            ))}
          </ul>
          <form className="d-flex me-3" onSubmit={(e) => e.preventDefault()}>
            <select
              className="form-select form-select-sm"
              aria-label="Business unit"
              value={businessUnitId}
              onChange={(e) => setBusinessUnitId(e.target.value)}
            >
              <option value="">All Businesses</option>
              {businessUnits.map((bu) => (
                <option key={bu.business_unit_id} value={bu.business_unit_id}>
                  {bu.business_unit_name}
                </option>
              ))}
            </select>
          </form>
          <span className="navbar-text text-light me-3 small">
            {user ? user.username : ''}
          </span>
          <button className="btn btn-outline-light btn-sm" onClick={handleLogout}>
            Sign out
          </button>
        </div>
      </nav>
      <main className="container-fluid flex-grow-1 py-4">{children}</main>
      <footer className="border-top py-2 text-center text-muted small">
        Ajaya Venture · AB Mushroom Farming · Ajaya Fish Farming
      </footer>
    </div>
  );
}