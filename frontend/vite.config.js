import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// /api wird an das Spring-Boot-Backend weitergeleitet (auch Video-Streaming und Poster)
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5175,
    strictPort: true,
    proxy: {
      '/api': 'http://localhost:8086',
    },
  },
});
