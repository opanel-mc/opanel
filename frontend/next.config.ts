import type { NextConfig } from "next";
import { randomUUID } from "node:crypto";
import { PHASE_DEVELOPMENT_SERVER } from "next/constants";
import { frontendDir, resolveFrontendTarget } from "./scripts/build-config.js";
import { resolveTextureVersions, writeTextureLoaders } from "./scripts/texture-config.js";

function createDeploymentId() {
  let id = randomUUID();
  while(/ad/i.test(id)) id = randomUUID();
  return id;
}

const deploymentId = createDeploymentId();

export default function nextConfig(phase: string): NextConfig {
  const production = phase !== PHASE_DEVELOPMENT_SERVER;
  const textureModule = writeTextureLoaders(resolveTextureVersions(process.env, production));

  return {
    deploymentId,
    generateBuildId: async () => deploymentId,
    output: "export",
    agentRules: false,
    trailingSlash: true,
    images: { unoptimized: true },
    // Keep the existing Gradle/Cargo variables for framework build comparisons.
    env: {
      NEXT_PUBLIC_OPANEL_TARGET: resolveFrontendTarget(process.env, production),
      NEXT_PUBLIC_OPANEL_VERSION: process.env.VITE_OPANEL_VERSION ?? "dev",
    },
    turbopack: {
      root: frontendDir,
      resolveAlias: { "opanel-textures": "./build/textures.js" },
    },
    webpack(config) {
      config.resolve.alias["opanel-textures"] = textureModule;
      return config;
    },
  };
}
