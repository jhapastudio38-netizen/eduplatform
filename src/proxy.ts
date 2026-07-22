/**
 * Security headers + lightweight middleware.
 *
 * - Sets CSP, X-Frame-Options, X-Content-Type-Options, Referrer-Policy,
 *   Permissions-Policy, HSTS (prod only).
 * - No business logic here — auth is done in the API routes via session cookies.
 *
 * For 100k concurrency on AWS, put CloudFront / Cloudflare in front and
 * push header enforcement to the edge. This middleware still runs as the
 * last-mile defence.
 */
import { NextResponse, NextRequest } from "next/server";

export function proxy(_req: NextRequest) {
  const res = NextResponse.next();

  const csp = [
    "default-src 'self'",
    "script-src 'self' 'unsafe-inline' 'unsafe-eval'",
    "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com",
    "font-src 'self' https://fonts.gstatic.com data:",
    "img-src 'self' data: blob: https:",
    "media-src 'self' blob: https:",
    "connect-src 'self' https: wss: ws:",
    "frame-ancestors 'none'",
    "form-action 'self'",
    "base-uri 'self'",
    "object-src 'none'",
  ].join("; ");

  res.headers.set("Content-Security-Policy", csp);
  res.headers.set("X-Frame-Options", "DENY");
  res.headers.set("X-Content-Type-Options", "nosniff");
  res.headers.set("Referrer-Policy", "strict-origin-when-cross-origin");
  res.headers.set("Permissions-Policy", "camera=(), microphone=(), geolocation=(), interest-cohort=()");
  res.headers.set("X-DNS-Prefetch-Control", "on");

  if (process.env.NODE_ENV === "production") {
    res.headers.set("Strict-Transport-Security", "max-age=63072000; includeSubDomains; preload");
  }

  return res;
}

export const config = {
  matcher: [
    // Run on everything except Next internals and static assets
    "/((?!_next/static|_next/image|favicon.ico|robots.txt|manifest.json|icons/).*)",
  ],
};
