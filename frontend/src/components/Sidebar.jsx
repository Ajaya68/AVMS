import { NavLink } from "react-router-dom";
import NAV from "../utils/navItems";

function SidebarContent({ onNavigate }) {
  return (
    <div className="d-flex flex-column h-100">
      <div className="sidebar-brand d-flex align-items-center gap-2 px-3 py-3">
        <i className="bi bi-asterisk fs-4 text-primary" />
        <div className="lh-1">
          <div className="fw-bold">AVMS</div>
          <small className="text-muted">Ajaya Venture Management</small>
        </div>
      </div>

      <nav className="flex-grow-1 overflow-auto px-2 pb-3">
        {NAV.map((group, idx) => (
          <div key={group.section ?? `g${idx}`} className="mb-2">
            {group.section && (
              <div className="sidebar-section px-2 mt-3 mb-1">
                {group.section}
              </div>
            )}
            {group.items.map((item) => (
              <NavLink
                key={item.path}
                to={item.path}
                end={item.path === "/"}
                className={({ isActive }) =>
                  `sidebar-link d-flex align-items-center gap-2 px-2 py-2 rounded mb-1 ${
                    isActive ? "active" : ""
                  }`
                }
                onClick={onNavigate}
              >
                <i className={`bi ${item.icon}`} />
                <span>{item.label}</span>
              </NavLink>
            ))}
          </div>
        ))}
      </nav>
    </div>
  );
}

export { SidebarContent };
export default SidebarContent;