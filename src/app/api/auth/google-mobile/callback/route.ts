/**
 * GET /api/auth/google-mobile/callback?code=...&state=...
 *
 * OAuth callback for the Android Google Sign-In flow. Google redirects here
 * with a one-time authorization code. We:
 *   1. Exchange the code for an ID token + access token at Google's token endpoint
 *   2. Decode the ID token to get the user's email + name + sub
 *   3. Find-or-create the DreamKorea user (signupMethod "google")
 *   4. Create a Session (so the cookie is set for any follow-up web request)
 *   5. Redirect back to the Android app via dreamkorea://auth-callback?userId=...&name=...&email=...&phone=...&role=...
 *
 * MainActivity's intent-filter + onNewIntent handler catches the redirect,
 * extracts the query params, saves the user profile via AppState, and flips
 * the UI from LoginScreen to MainScreen.
 *
 * Required env vars:
 *   GOOGLE_CLIENT_ID
 *   GOOGLE_CLIENT_SECRET
 *   NEXT_PUBLIC_APP_URL
 */
import { NextRequest, NextResponse } from "next/server";
import { db } from "@/lib/db";
import { createSession } from "@/lib/session";
import { audit } from "@/lib/audit";
import { CONFIG } from "@/lib/config";

interface GoogleTokenResponse {
  id_token?: string;
  access_token?: string;
  error?: string;
  error_description?: string;
}

interface JwtPayload {
  email?: string;
  email_verified?: string | boolean;
  name?: string;
  picture?: string;
  sub?: string;
}

function decodeJwtPayload(jwt: string): JwtPayload {
  try {
    const parts = jwt.split(".");
    if (parts.length < 2) return {};
    // base64url → base64 → JSON
    const b64 = parts[1].replace(/-/g, "+").replace(/_/g, "/");
    const json = Buffer.from(b64, "base64").toString("utf8");
    return JSON.parse(json) as JwtPayload;
  } catch {
    return {};
  }
}

export async function GET(req: NextRequest) {
  const code = req.nextUrl.searchParams.get("code");
  const error = req.nextUrl.searchParams.get("error");

  if (error) {
    // User declined or Google errored — bounce back to the app with an error
    return NextResponse.redirect(
      `dreamkorea://auth-callback?error=${encodeURIComponent(error)}`
    );
  }
  if (!code) {
    return NextResponse.redirect(
      `dreamkorea://auth-callback?error=missing_code`
    );
  }

  const clientId = process.env.GOOGLE_CLIENT_ID;
  const clientSecret = process.env.GOOGLE_CLIENT_SECRET;
  const redirectUri = `${CONFIG.app.url}/api/auth/google-mobile/callback`;

  if (!clientId || !clientSecret) {
    return NextResponse.redirect(
      `dreamkorea://auth-callback?error=server_misconfigured`
    );
  }

  // ── Exchange the code for tokens ───────────────────────────────────────
  let tokenResp: GoogleTokenResponse;
  try {
    const params = new URLSearchParams({
      code,
      client_id: clientId,
      client_secret: clientSecret,
      redirect_uri: redirectUri,
      grant_type: "authorization_code",
    });
    const r = await fetch("https://oauth2.googleapis.com/token", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: params.toString(),
    });
    tokenResp = (await r.json()) as GoogleTokenResponse;
  } catch {
    return NextResponse.redirect(
      `dreamkorea://auth-callback?error=token_exchange_failed`
    );
  }

  if (tokenResp.error || !tokenResp.id_token) {
    return NextResponse.redirect(
      `dreamkorea://auth-callback?error=${encodeURIComponent(
        tokenResp.error || "no_id_token"
      )}`
    );
  }

  // ── Decode the ID token (already verified by Google's token exchange) ──
  const payload = decodeJwtPayload(tokenResp.id_token);
  if (!payload.email || (payload.email_verified !== "true" && payload.email_verified !== true)) {
    return NextResponse.redirect(
      `dreamkorea://auth-callback?error=email_not_verified`
    );
  }

  const email = payload.email.toLowerCase();
  const name = payload.name || email.split("@")[0];
  const avatarUrl = payload.picture || null;
  const ip = req.headers.get("x-forwarded-for")?.split(",")[0] || "unknown";

  // Find-or-create user
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
  }

  await createSession(user.id);
  await audit({
    actorId: user.id,
    action: "login",
    entity: "User",
    entityId: user.id,
    ip,
  });

  // ── Redirect back to the Android app with the user profile ────────────
  const callbackParams = new URLSearchParams({
    userId: user.id,
    name: user.name || "",
    email: user.email,
    phone: user.phone || "",
    role: user.role,
  });
  return NextResponse.redirect(
    `dreamkorea://auth-callback?${callbackParams.toString()}`
  );
}
