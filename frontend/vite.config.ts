import path from "node:path";
import vinext from "vinext";
import { defineConfig, loadEnv } from "vite";
import { frontendDir, resolveFrontendTarget } from "./scripts/build-config.js";

export default defineConfig(({ command, mode }) => ({
  define: {
    "import.meta.env.VITE_OPANEL_TARGET": JSON.stringify(resolveFrontendTarget(
      { ...loadEnv(mode, frontendDir, ""), ...process.env },
      command === "build",
    )),
  },
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
  build: {
    rolldownOptions: {
      checks: {
        pluginTimings: false,
        filenameConflict: false
      }
    }
  }
}));
