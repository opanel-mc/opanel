import vinext from "vinext";
import { defineConfig, loadEnv } from "vite";
import { frontendDir, resolveBulletinBaseUrl, resolveFrontendTarget } from "./scripts/build-config.js";
import { texturesPlugin } from "./vite-plugins/textures-plugin.js";
import { resolveTextureVersions } from "./scripts/texture-config.js";

export default defineConfig(({ command, mode }) => {
  const env = { ...loadEnv(mode, frontendDir, ""), ...process.env };
  const production = command === "build";

  return {
    define: {
      "import.meta.env.VITE_OPANEL_TARGET": JSON.stringify(resolveFrontendTarget(env, production)),
      "import.meta.env.VITE_BULLETIN_BASE_URL": JSON.stringify(resolveBulletinBaseUrl(env)),
    },
    plugins: [texturesPlugin(resolveTextureVersions(env, production)), vinext()],
    ssr: {
      // semver and tweetnacl are CommonJS. Keeping them external avoids Vite's dev SSR
      // module runner applying an incompatible CommonJS transform to their internals.
      external: ["semver", "tweetnacl"],
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
