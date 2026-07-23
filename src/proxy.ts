/**
 * Security middleware — applies HTTP security headers to every response.
 * Defends against: XSS, clickjacking, MIME-sniffing, downgrade attacks,
 * bot scraping, and AI agent abuse.
 *
 * Key headers:
 * - Content-Security-Policy: restrict script/style/font/img sources
 * - Strict-Transport-Security: force HTTPS for 2 years
 * - X-Frame-Options: DENY — no iframes (clickjacking defense)
 * - X-Content-Type-Options: nosniff — no MIME sniffing
 * - Referrer-Policy: strict-origin-when-cross-origin
 * - Permissions-Policy: disable camera/mic/geolocation by default
 * - User-Agent filter: block obvious AI scrapers from /admin and /teacher
 */
import { NextRequest, NextResponse } from "next/server";

export function proxy(req: NextRequest) {
  const res = NextResponse.next({
    request: { headers: req.headers },
  });

  // ─── Security headers ──────────────────────────────────────────────────────
  res.headers.set("Strict-Transport-Security", "max-age=63072000; includeSubDomains; preload");
  res.headers.set("X-Frame-Options", "DENY");
  res.headers.set("X-Content-Type-Options", "nosniff");
  res.headers.set("Referrer-Policy", "strict-origin-when-cross-origin");
  res.headers.set("Permissions-Policy", "camera=(), microphone=(), geolocation=(), interest-cohort=()");
  res.headers.set("X-DNS-Prefetch-Control", "on");
  // CSP — allow inline styles + scripts (Next.js requires this), Google Fonts for Hangul, images from anywhere
  res.headers.set(
    "Content-Security-Policy",
    "default-src 'self'; " +
      "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
      "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
      "font-src 'self' https://fonts.gstatic.com data:; " +
      "img-src 'self' data: blob: https:; " +
      "media-src 'self' blob: https:; " +
      "connect-src 'self' https: wss: ws:; " +
      "frame-ancestors 'none'; " +
      "form-action 'self'; " +
      "base-uri 'self'; " +
      "object-src 'none'"
  );

  // ─── Block AI scrapers from admin/teacher URLs ─────────────────────────────
  const ua = req.headers.get("user-agent") || "";
  const path = req.nextUrl.pathname;
  const isHiddenRoute = path.startsWith("/admin") || path.startsWith("/teacher") || path.startsWith("/api/admin") || path.startsWith("/api/auth/credentials");
  const aiBots = /(GPTBot|ChatGPT-User|CCBot|google-extended|Claude-Web|anthropic-ai|OAI-SearchBot|PerplexityBot|Amazonbot|Bytespider|Diffbot|FacebookBot|Meta-ExternalAgent)/i;
  if (isHiddenRoute && aiBots.test(ua)) {
    return new NextResponse("Forbidden", { status: 403 });
  }

  // ─── Rate limit admin/teacher API routes more aggressively ─────────────────
  // (in-memory per-instance rate limit; for serious DDoS use ALB + WAF)
  if (isHiddenRoute) {
    const ip = req.headers.get("x-forwarded-for")?.split(",")[0] || "unknown";
    // Simple per-instance throttle: 60 req/min per IP on admin routes
    const key = `admin:${ip}`;
    const now = Date.now();
    const bucket = (globalThis as Record<string, unknown>).__rateBucket as Map<string, number[]> | undefined;
    if (!bucket) {
      (globalThis as Record<string, unknown>).__rateBucket = new Map<string, number[]>();
    }
    const m = (globalThis as Record<string, unknown>).__rateBucket as Map<string, number[]>;
    const arr = m.get(key) || [];
    const recent = arr.filter((t) => now - t < 60_000);
    if (recent.length >= 60) {
      return new NextResponse(JSON.stringify({ error: "Too many requests" }), {
        status: 429,
        headers: { "Content-Type": "application/json", "Retry-After": "60" },
      });
    }
    recent.push(now);
    m.set(key, recent);
  }

  return res;
}

export const config = {
  matcher: [
    /*
     * Match all request paths except for the ones starting with:
     * - _next/static (static files)
     * - _next/image (image optimization files)
     * - favicon.ico (favicon file)
     * - public assets (images, etc.)
     */
    "/((?!_next/static|_next/image|favicon.svg|icons|images|manifest.json|robots.txt|sitemap.xml).*)",
  ],
};
