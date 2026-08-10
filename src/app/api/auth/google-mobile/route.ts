/**
 * GET /api/auth/google-mobile
 *
 * Entry point for the Android Google Sign-In flow. Redirects the user's
 * Chrome Custom Tab to Google's OAuth consent screen. After consent, Google
 * redirects to /api/auth/google-mobile/callback which exchanges the code
 * for credentials and bounces back to the app via the dreamkorea:// scheme.
 *
 * Required env vars:
 *   GOOGLE_CLIENT_ID      — OAuth client ID (Web application type)
 *   GOOGLE_CLIENT_SECRET  — OAuth client secret
 *   NEXT_PUBLIC_APP_URL   — Public base URL of this app (e.g. https://my-project-five-sepia.vercel.app)
 *
 * If env vars are missing, returns a 500 with a helpful message instead of
 * silently redirecting to a broken Google URL.
 */
import { NextResponse } from "next/server";
import { CONFIG } from "@/lib/config";

export async function GET() {
  const clientId = process.env.GOOGLE_CLIENT_ID;
  const redirectUri = `${CONFIG.app.url}/api/auth/google-mobile/callback`;

  if (!clientId) {
    return NextResponse.json(
      {
        error:
          "Google OAuth is not configured. Set GOOGLE_CLIENT_ID in environment variables.",
      },
      { status: 500 }
    );
  }

  const params = new URLSearchParams({
    client_id: clientId,
    redirect_uri: redirectUri,
    response_type: "code",
    scope: "openid email profile",
    access_type: "online",
    prompt: "select_account",
  });

  return NextResponse.redirect(
    `https://accounts.google.com/o/oauth2/v2/auth?${params.toString()}`
  );
}
