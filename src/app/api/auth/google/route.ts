/**
 * POST /api/auth/google
 * Body: { idToken: string }
 *
 * Verifies a Google ID token (sent by the Android client after a successful
 * Google Sign-In flow) and creates / looks up the corresponding DreamKorea
 * user. Returns the user profile + sets the ep_sid session cookie.
 *
 * Flow:
 *   1. Validate body
 *   2. Rate-limit per IP
 *   3. Verify the ID token with Google's tokeninfo endpoint
 *   4. Find-or-create user by email (signupMethod "google")
 *   5. Create Session, set cookie
 *   6. Return { ok, user }
 *
 * Returns:
 *   200 { ok: true, user }
 *   400 — missing/invalid token
 *   401 — token verification failed
 *   429 — rate limited
 */
import { NextRequest, NextResponse } from "next/server";
import { db } from "@/lib/db";
import { rateLimitKey } from "@/lib/security";
import { rateLimited } from "@/lib/rate-limit";
import { createSession } from "@/lib/session";
import { audit } from "@/lib/audit";

interface GoogleTokenInfo {
  email?: string;
  email_verified?: string | boolean;
  name?: string;
  picture?: string;
  sub?: string;
  aud?: string;
}

export async function POST(req: NextRequest) {
  let body: unknown;
  try {
    body = await req.json();
  } catch {
    return NextResponse.json({ error: "Invalid JSON" }, { status: 400 });
  }
  const b = body as { idToken?: unknown };
  const idToken = typeof b.idToken === "string" ? b.idToken.trim() : "";

  if (!idToken) {
    return NextResponse.json({ error: "Missing Google ID token" }, { status: 400 });
  }

  const ip = req.headers.get("x-forwarded-for")?.split(",")[0] || "unknown";
  if (rateLimited(rateLimitKey("google-ip", ip), 20, 3600)) {
    return NextResponse.json(
      { error: "Too many requests. Try again later." },
      { status: 429 }
    );
  }

  // Verify the ID token with Google.
  // We use the tokeninfo endpoint (no SDK dependency). For production with
  // high traffic, switch to the google-auth-library + cached Google certs.
  let tokenInfo: GoogleTokenInfo;
  try {
    const resp = await fetch(
      `https://oauth2.googleapis.com/tokeninfo?id_token=${encodeURIComponent(idToken)}`,
      { method: "GET" }
    );
    if (!resp.ok) {
      return NextResponse.json(
        { error: "Google token verification failed" },
        { status: 401 }
      );
    }
    tokenInfo = (await resp.json()) as GoogleTokenInfo;
  } catch {
    return NextResponse.json(
      { error: "Could not reach Google to verify token" },
      { status: 502 }
    );
  }

  if (!tokenInfo.email || tokenInfo.email_verified !== "true" && tokenInfo.email_verified !== true) {
    return NextResponse.json(
      { error: "Google account email is not verified" },
      { status: 400 }
    );
  }

  const email = tokenInfo.email.toLowerCase();
  const name = tokenInfo.name || email.split("@")[0];
  const avatarUrl = tokenInfo.picture || null;

  // Find-or-create user by email
  let user = await db.user.findUnique({ where: { email } });
  if (!user) {
    user = await db.user.create({
      data: {
        email,
        name,
        role: "STUDENT",
        isVerified: true,
        signupMethod: "google",
        avatarUrl,
      },
    });
  } else if (user.isBanned) {
    return NextResponse.json(
      { error: "Account is suspended. Contact administrator." },
      { status: 403 }
    );
  }

  await createSession(user.id);
  await audit({
    actorId: user.id,
    action: "login",
    entity: "User",
    entityId: user.id,
    ip,
  });

  return NextResponse.json({
    ok: true,
    user: {
      id: user.id,
      name: user.name,
      email: user.email,
      phone: user.phone,
      role: user.role,
    },
  });
}
