import { Dropdown } from "react-bootstrap";
import { useNavigate } from "react-router-dom";

import { useAuth } from "../context/AuthContext";

function Topbar({ onToggleSidebar }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate("/login", { replace: true });
  };

  const displayName = user?.full_name || user?.email || "Guest";
  const roles = user?.role_codes?.length ? user.role_codes.join(", ") : "User";

  return (
    <header className="navbar navbar-expand topbar d-flex flex-row shadow-sm px-3">
      <button
        className="btn btn-link d-lg-none text-body p-1 me-2"
        type="button"
        aria-label="Toggle sidebar"
        onClick={onToggleSidebar}
      >
        <i className="bi bi-list fs-3" />
      </button>

      <div className="d-none d-sm-block text-muted">
        <i className="bi bi-calendar3 me-1" />
        {new Date().toLocaleDateString(undefined, {
          weekday: "long",
          day: "numeric",
          month: "long",
          year: "numeric",
        })}
      </div>

      <div className="ms-auto d-flex align-items-center gap-3">
        <button
          className="btn btn-link position-relative text-body p-1"
          type="button"
          aria-label="Notifications"
        >
          <i className="bi bi-bell fs-5" />
          <span className="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger d-none">
            0
          </span>
        </button>

        <Dropdown align="end">
          <Dropdown.Toggle
            as="button"
            className="btn btn-link text-body text-decoration-none d-flex align-items-center gap-2"
            id="user-menu"
          >
            <div className="rounded-circle bg-primary text-white d-flex align-items-center justify-content-center user-avatar">
              {displayName.charAt(0).toUpperCase()}
            </div>
            <div className="lh-1 text-start d-none d-md-block">
              <div className="fw-semibold small">{displayName}</div>
              <small className="text-muted">{roles}</small>
            </div>
            <i className="bi bi-chevron-down small d-none d-md-block" />
          </Dropdown.Toggle>

          <Dropdown.Menu>
            <Dropdown.Item onClick={handleLogout}>
              <i className="bi bi-box-arrow-right me-2" />
              Sign out
            </Dropdown.Item>
          </Dropdown.Menu>
        </Dropdown>
      </div>
    </header>
  );
}

export default Topbar;