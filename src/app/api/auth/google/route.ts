/**
 * POST /api/auth/google
 * Body: { idToken: string }
 *
 * - Verifies the Google ID token using Google's tokeninfo endpoint
 * - Extracts user info (email, name, picture)
 * - Creates or updates User in DB (signupMethod = "google")
 * - Creates session, sets cookie
 * - Returns user credentials (same shape as /api/auth/credentials)
 */
import { NextRequest, NextResponse } from "next/server";
import { db } from "@/lib/db";
import { createSession } from "@/lib/session";
import { audit } from "@/lib/audit";
import { hashPassword } from "@/lib/password";

export async function POST(req: NextRequest) {
  let body: unknown;
  try {
    body = await req.json();
  } catch {
    return NextResponse.json({ error: "Invalid JSON" }, { status: 400 });
  }

  const { idToken } = body as { idToken?: string };
  if (!idToken || typeof idToken !== "string") {
    return NextResponse.json({ error: "Missing Google ID token" }, { status: 400 });
  }

  // Verify the Google ID token using Google's tokeninfo endpoint
  let googleUser: { email?: string; name?: string; picture?: string; sub?: string };
  try {
    const resp = await fetch(
      `https://oauth2.googleapis.com/tokeninfo?id_token=${encodeURIComponent(idToken)}`,
    );
    if (!resp.ok) {
      return NextResponse.json({ error: "Invalid Google token" }, { status: 401 });
    }
    googleUser = await resp.json();
  } catch {
    return NextResponse.json({ error: "Could not verify Google token" }, { status: 502 });
  }

  const email = googleUser.email?.toLowerCase().trim();
  if (!email) {
    return NextResponse.json({ error: "Google account has no email" }, { status: 400 });
  }

  const name = googleUser.name || email.split("@")[0];
  const ip = req.headers.get("x-forwarded-for")?.split(",")[0] || "unknown";

  // Find or create user
  let user = await db.user.findFirst({ where: { email } });

  if (!user) {
    // Create new user with Google info
    // Generate a random password hash (Google users don't use password login)
    const randomPassword = crypto.randomUUID() + crypto.randomUUID();
    const passwordHash = await hashPassword(randomPassword);
    user = await db.user.create({
      data: {
        email,
        name,
        passwordHash,
        role: "STUDENT",
        isVerified: true,
        isBanned: false,
        signupMethod: "google",
      },
    });
  } else {
    if (user.isBanned) {
      return NextResponse.json({ error: "Account is suspended. Contact administrator." }, { status: 403 });
    }
    // Update name if user didn't have one
    if (!user.name || user.name !== name) {
      user = await db.user.update({
        where: { id: user.id },
        data: { name, isVerified: true },
      });
    }
  }

  // Create session
  await createSession(user.id);
  await audit({
    actorId: user.id,
    action: "login_google",
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
      username: user.username,
    },
  });
}
