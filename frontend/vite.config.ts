import path from "node:path";
import vinext from "vinext";
import { defineConfig } from "vite";

export default defineConfig({
  plugins: [vinext()],
  ssr: {
    // semver is CommonJS. Keeping it external avoids Vite's dev SSR module
    // runner applying an incompatible CommonJS transform to its internals.
    external: ["semver"],
  },
  resolve: {
    // `minecraft-textures` exports JSON files individually, but the dynamic
    // import in lib/texture.ts needs Vite to enumerate the containing folder.
    alias: {
      "@minecraft-textures-json": path.resolve(
        "node_modules/minecraft-textures/dist/textures/json"
      ),
    },
  },
});
