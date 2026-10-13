import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

// In development every /api call is proxied to the Spring Boot backend, so the browser
// sees a single origin and the backend needs no CORS configuration.
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  return {
    plugins: [react()],
    server: {
      port: 5173,
      proxy: {
        '/api': {
          target: env.BACKEND_URL ?? 'http://localhost:8080',
          changeOrigin: true,
        },
      },
    },
  };
});
