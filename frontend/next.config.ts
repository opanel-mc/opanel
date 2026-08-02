import type { NextConfig } from "next";
import { randomUUID } from "node:crypto";

function createDeploymentId() {
  let id = randomUUID();
  while(/ad/i.test(id)) id = randomUUID();
  return id;
}

const deploymentId = createDeploymentId();

const nextConfig: NextConfig = {
  deploymentId,
  generateBuildId: async () => deploymentId,
  output: "export",
  // vinext beta currently follows its own trailing-slash redirect as a
  // dynamic response while prerendering. The bundle step restores the
  // route/index.html layout expected by Javalin.
  trailingSlash: false,
};

export default nextConfig;
