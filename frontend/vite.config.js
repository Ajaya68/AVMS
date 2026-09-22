import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// See `.env.example` for server configuration. The dev server proxies /api to
// the Jakarta Servlet backend. In production Nginx performs this proxy.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: process.env.VITE_API_TARGET || 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
    chunkSizeWarningLimit: 1200,
  },
});