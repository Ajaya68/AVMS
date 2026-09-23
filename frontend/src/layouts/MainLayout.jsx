import { useState } from "react";
import { Offcanvas } from "react-bootstrap";
import { Outlet } from "react-router-dom";
import SidebarContent from "../components/Sidebar";
import Topbar from "../components/Topbar";
import { useAuth } from "../context/AuthContext";

function MainLayout() {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  useAuth(); // future-proof: layout re-renders on auth changes

  return (
    <div className="app-shell">
      {/* Static sidebar (lg and up) */}
      <aside className="app-sidebar d-none d-lg-flex flex-column">
        <SidebarContent />
      </aside>

      {/* Offcanvas sidebar (mobile) */}
      <Offcanvas
        show={sidebarOpen}
        onHide={() => setSidebarOpen(false)}
        className="app-offcanvas"
      >
        <SidebarContent onNavigate={() => setSidebarOpen(false)} />
      </Offcanvas>

      <div className="app-main d-flex flex-column">
        <Topbar onToggleSidebar={() => setSidebarOpen(true)} />
        <main className="app-content p-3 p-lg-4">
          <Outlet />
        </main>
      </div>
    </div>
  );
}

export default MainLayout;