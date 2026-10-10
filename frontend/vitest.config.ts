import path from "node:path";
import { fileURLToPath } from "node:url";
import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";
import { texturesPlugin } from "./vite-plugins/textures-plugin.js";
import { resolveTextureVersions } from "./scripts/texture-config.js";

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
    env: {
      NEXT_PUBLIC_OPANEL_VERSION: "0.1.0",
      NEXT_PUBLIC_OPANEL_TARGET: "paper-26.1"
    },
    setupFiles: ["./test/setup.tsx"]
  },
  plugins: [react(), texturesPlugin(resolveTextureVersions({ TEXTURE_VERSIONS: "all" }))]
});
