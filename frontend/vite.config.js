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
      // Explicit HMR channel so file saves push updates to the browser
      // automatically (no manual refresh). Uses a plain ws:// connection
      // on the same port as the dev server.
      hmr: {
        host: "localhost",
        port: 5173,
        protocol: "ws",
      },
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