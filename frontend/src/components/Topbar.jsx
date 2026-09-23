import { useAuth } from "../context/AuthContext";

function Topbar({ onToggleSidebar }) {
  const { user } = useAuth();

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
        <button className="btn btn-link position-relative text-body p-1" type="button" aria-label="Notifications">
          <i className="bi bi-bell fs-5" />
          <span className="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger d-none">0</span>
        </button>
        <div className="d-flex align-items-center gap-2">
          <div className="rounded-circle bg-primary text-white d-flex align-items-center justify-content-center user-avatar">
            {user ? user.email?.charAt(0).toUpperCase() : "A"}
          </div>
          <div className="lh-1 d-none d-md-block">
            <div className="fw-semibold small">{user ? user.email : "Guest"}</div>
            <small className="text-muted">{user ? "User" : "Not signed in"}</small>
          </div>
        </div>
      </div>
    </header>
  );
}

export default Topbar;