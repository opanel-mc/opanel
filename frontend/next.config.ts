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
  trailingSlash: true,
};

export default nextConfig;
