import vinext from "vinext";
import { defineConfig, loadEnv } from "vite";
import { frontendDir, resolveFrontendTarget } from "./scripts/build-config.js";
import { texturesPlugin } from "./vite-plugins/textures-plugin.js";
import { resolveTextureVersions } from "./scripts/texture-config.js";

export default defineConfig(({ command, mode }) => {
  const env = { ...loadEnv(mode, frontendDir, ""), ...process.env };
  const production = command === "build";

  return {
    define: {
      "import.meta.env.VITE_OPANEL_TARGET": JSON.stringify(resolveFrontendTarget(env, production)),
    },
    plugins: [texturesPlugin(resolveTextureVersions(env, production)), vinext()],
    ssr: {
      // semver is CommonJS. Keeping it external avoids Vite's dev SSR module
      // runner applying an incompatible CommonJS transform to its internals.
      external: ["semver"],
    },
    build: {
      rolldownOptions: {
        checks: {
          pluginTimings: false,
          filenameConflict: false
        }
      }
    }
  };
});
