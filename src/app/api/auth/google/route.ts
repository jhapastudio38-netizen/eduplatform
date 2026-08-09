/**
 * POST /api/auth/google
 * Body: { idToken: string }
 *
 * Verifies Google ID token and creates/finds user.
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

  // Verify the Google ID token
  let googleUser: { email?: string; name?: string };
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

  let user = await db.user.findFirst({ where: { email } });

  if (!user) {
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
      return NextResponse.json({ error: "Account is suspended" }, { status: 403 });
    }
  }

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
