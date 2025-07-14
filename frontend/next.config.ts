import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  distDir: "build",
  output: "export",
  trailingSlash: true,
  skipTrailingSlashRedirect: true,
  reactStrictMode: false
};

export default nextConfig;
