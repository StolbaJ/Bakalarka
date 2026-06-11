import type { NextConfig } from "next";

/** V Dockeru nastav při buildu na http://backend:8080 (viz docker-compose.dev.yml). */
const apiProxyTarget = (process.env.API_PROXY_TARGET || 'http://localhost:8080').replace(/\/$/, '')

const nextConfig: NextConfig = {
  output: 'standalone',
  async rewrites() {
    return [
      { source: '/api/:path*', destination: `${apiProxyTarget}/api/:path*` },
    ];
  },
};

export default nextConfig;
