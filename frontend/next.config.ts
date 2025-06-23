import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  distDir: "build",
  output: "export",
  trailingSlash: true,
  skipTrailingSlashRedirect: true,
};

export default nextConfig;
