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
    }
  },
  test: {
    environment: "jsdom",
    setupFiles: ["./test/setup.ts"]
  },
  plugins: [react()]
});
