import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";
import { Toast, ToastContainer } from "react-bootstrap";

const ToastContext = createContext(null);

let nextId = 1;

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);

  const remove = useCallback((id) => {
    setToasts((list) => list.filter((t) => t.id !== id));
  }, []);

  const push = useCallback(
    (variant, message) => {
      const id = nextId++;
      setToasts((list) => [...list, { id, variant, message }]);
      window.setTimeout(() => remove(id), 4500);
    },
    [remove]
  );

  // Listen for toast events broadcast by the api interceptor.
  useEffect(() => {
    const handler = (event) => {
      push(event.detail.variant, event.detail.message);
    };
    window.addEventListener("avms:toast", handler);
    return () => window.removeEventListener("avms:toast", handler);
  }, [push]);

  const api = useMemo(
    () => ({
      success: (message) => push("success", message),
      error: (message) => push("danger", message),
      info: (message) => push("info", message),
    }),
    [push]
  );

  return (
    <ToastContext.Provider value={api}>
      {children}
      <ToastContainer position="top-end" className="p-3" style={{ zIndex: 2000 }}>
        {toasts.map((t) => (
          <Toast key={t.id} bg={t.variant} onClose={() => remove(t.id)} autohide delay={4500}>
            <Toast.Body className="text-white d-flex align-items-center gap-2">
              <i className={`bi ${t.variant === "danger" ? "bi-exclamation-triangle-fill" : "bi-check-circle-fill"}`} />
              <span>{t.message}</span>
            </Toast.Body>
          </Toast>
        ))}
      </ToastContainer>
    </ToastContext.Provider>
  );
}

// eslint-disable-next-line react-refresh/only-export-components
export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) {
    throw new Error("useToast must be used within ToastProvider");
  }
  return ctx;
}