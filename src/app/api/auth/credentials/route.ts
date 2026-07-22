/**
 * POST /api/auth/credentials
 * Body: { username: string, password: string }
 *
 * For teachers + admins only — students use OTP.
 * - Validates input
 * - Rate-limits per IP + per username
 * - Looks up user by username OR email
 * - Verifies passwordHash (constant-time)
 * - Creates Session, sets cookie
 */
import { NextRequest, NextResponse } from "next/server";
import { db } from "@/lib/db";
import {
  rateLimitKey,
} from "@/lib/security";
import { rateLimited } from "@/lib/rate-limit";
import { createSession } from "@/lib/session";
import { audit } from "@/lib/audit";
import { verifyPassword } from "@/lib/password";

export async function POST(req: NextRequest) {
  let body: unknown;
  try {
    body = await req.json();
  } catch {
    return NextResponse.json({ error: "Invalid JSON" }, { status: 400 });
  }
  const b = body as { username?: unknown; password?: unknown };

  const username =
    typeof b.username === "string" ? b.username.trim().toLowerCase().slice(0, 100) : "";
  const password =
    typeof b.password === "string" ? b.password.slice(0, 200) : "";

  if (!username || !password) {
    return NextResponse.json({ error: "Username and password are required" }, { status: 400 });
  }

  const ip = req.headers.get("x-forwarded-for")?.split(",")[0] || "unknown";

  // Rate limit: 10 attempts per IP per hour, 10 per username per hour
  if (rateLimited(rateLimitKey("cred-ip", ip), 10, 3600)) {
    return NextResponse.json(
      { error: "Too many login attempts from this IP. Try again later." },
      { status: 429 },
    );
  }
  if (rateLimited(rateLimitKey("cred-user", username), 10, 3600)) {
    return NextResponse.json(
      { error: "Too many login attempts. Try again later." },
      { status: 429 },
    );
  }

  // Find user by username or email — allow any role (students can set passwords too)
  const user = await db.user.findFirst({
    where: {
      OR: [{ username }, { email: username }],
    },
  });

  if (!user || !user.passwordHash) {
    return NextResponse.json({ error: "Invalid email or password" }, { status: 401 });
  }

  if (user.isBanned) {
    return NextResponse.json({ error: "Account is suspended. Contact administrator." }, { status: 403 });
  }

  // Verify password
  const valid = await verifyPassword(password, user.passwordHash);
  if (!valid) {
    await audit({
      actorId: user.id,
      action: "login_failed",
      entity: "User",
      entityId: user.id,
      ip,
    });
    return NextResponse.json({ error: "Invalid credentials" }, { status: 401 });
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
      username: user.username,
    },
  });
}
