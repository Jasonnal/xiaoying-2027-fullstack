import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  reactStrictMode: true,
  // 容器交付：产出 .next/standalone，运行时不需要 node_modules
  output: "standalone",
  poweredByHeader: false,
};

export default nextConfig;
