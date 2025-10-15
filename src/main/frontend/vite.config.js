import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  server: { port: 5174 },   // Currently running on 5174, so fix it
  plugins: [react()],
  server: {
    proxy: {
      // Let /api be proxied from local dev server to Render, avoiding browser CORS
      '/api': {
        target: 'https://null-pointers-gbfc.onrender.com',
        changeOrigin: true,
        secure: true,
      }
    }
  }
})
