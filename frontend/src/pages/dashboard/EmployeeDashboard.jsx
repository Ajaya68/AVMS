import { Alert } from "react-bootstrap";

import LoadingSpinner from "../../components/LoadingSpinner";
import { useApi } from "../../hooks/useApi";
import { fetchMyProfile } from "../../services/employeeService";
import MyAttendancePanel from "./MyAttendancePanel";
import MyProfileCard from "./MyProfileCard";
import { DashboardHeader, QuickActions } from "./widgets";
import { PERSONA_META } from "./persona";
import { useAuth } from "../../context/AuthContext";

/**
 * Employee dashboard: personal profile, check in/out and the full
 * month attendance calendar. Shown to EMPLOYEE-role users and anyone
 * whose roles grant no business modules.
 */
function EmployeeDashboard() {
  const meta = PERSONA_META.employee;
  const { hasPerm } = useAuth();
  const { data: profile, loading } = useApi(() => fetchMyProfile(), []);

  return (
    <div>
      <DashboardHeader title={meta.title} subtitle={meta.subtitle} />
      <MyProfileCard />
      <QuickActions
        actions={[
          { to: "/attendance", label: "Mark / View Attendance", icon: "bi-calendar-check", variant: "primary" },
          ...(hasPerm("notifications.view") ? [{ to: "/notifications", label: "Notifications", icon: "bi-bell" }] : []),
          ...(hasPerm("settings.view") ? [{ to: "/settings", label: "Settings", icon: "bi-gear" }] : []),
        ]}
      />
      {loading ? (
        <LoadingSpinner label="Loading your dashboard..." />
      ) : (
        <>
          <MyAttendancePanel />
          {!profile?.linked && (
            <Alert variant="info">
              Your login is not linked to an employee record yet, so no
              attendance is shown. Ask your administrator to link it from
              the Employees page — then your check-ins and monthly history
              will appear here.
            </Alert>
          )}
        </>
      )}
    </div>
  );
}

export default EmployeeDashboard;
