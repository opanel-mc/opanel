import path from "node:path";
import { fileURLToPath } from "node:url";
import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";

const __dirname = path.dirname(fileURLToPath(import.meta.url));

export default defineConfig({
  resolve: {
    alias: {
      "@/style/item-effect.css": path.resolve(__dirname, "test/style-stub.ts"),
      "@": path.resolve(__dirname, "."),
      "next/dynamic": "vinext/shims/dynamic",
      "next/font/local": "vinext/shims/font-local",
      "next/link": "vinext/shims/link",
      "next/navigation": "vinext/shims/navigation",
    }
  },
  test: {
    environment: "jsdom",
    setupFiles: ["./test/setup.tsx"]
  },
  plugins: [react()]
});
