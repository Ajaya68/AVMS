import { RouterProvider } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";
import { ToastProvider } from "./context/ToastContext";
import { VentureProvider } from "./context/VentureContext";
import router from "./routes/AppRoutes";

function App() {
  return (
    <AuthProvider>
      <ToastProvider>
        <VentureProvider>
          <RouterProvider router={router} />
        </VentureProvider>
      </ToastProvider>
    </AuthProvider>
  );
}

export default App;