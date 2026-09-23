import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const apiTarget = env.VITE_API_URL || "http://localhost:8000";

  return {
    plugins: [react()],
    server: {
      port: 5173,
      proxy: {
        // In dev, /api requests are forwarded to Django so the browser
        // never needs to deal with CORS.
        "/api": {
          target: apiTarget,
          changeOrigin: true,
        },
      },
    },
  };
});