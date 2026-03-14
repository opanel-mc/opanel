import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  distDir: "build",
  output: "export",
  trailingSlash: true,
  skipTrailingSlashRedirect: true,
  reactStrictMode: false,
  allowedDevOrigins: ["127.0.0.1"]
};

export default nextConfig;
