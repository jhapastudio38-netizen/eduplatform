/* Build v3 — merged auth route */
/**
 * POST /api/auth/credentials
 * Body: { username, password } for login
 *    OR { mode: "signup", name, email, phone, password } for signup
 *    OR { mode: "google", idToken } for Google sign-in
 *
 * Handles login, signup, and Google sign-in all in one route
 * (to avoid Vercel deployment issues with new route files).
 */
import { NextRequest, NextResponse } from "next/server";
import { db } from "@/lib/db";
import { rateLimitKey } from "@/lib/security";
import { rateLimited } from "@/lib/rate-limit";
import { createSession } from "@/lib/session";
import { audit } from "@/lib/audit";
import { verifyPassword, hashPassword } from "@/lib/password";

export async function POST(req: NextRequest) {
  let body: unknown;
  try { body = await req.json(); } catch { return NextResponse.json({ error: "Invalid JSON" }, { status: 400 }); }
  const b = body as Record<string, unknown>;

  const ip = req.headers.get("x-forwarded-for")?.split(",")[0] || "unknown";

  // ─── SIGNUP MODE ───
  if (b.mode === "signup" || b.signup === true) {
    const name = typeof b.name === "string" ? b.name.trim().slice(0, 100) : "";
    const email = typeof b.email === "string" ? b.email.trim().toLowerCase().slice(0, 200) : "";
    const phone = typeof b.phone === "string" ? b.phone.trim().slice(0, 30) : "";
    const password = typeof b.password === "string" ? b.password.slice(0, 200) : "";

    if (!name) return NextResponse.json({ error: "Name is required" }, { status: 400 });
    if (!email || !email.includes("@")) return NextResponse.json({ error: "Valid email is required" }, { status: 400 });
    if (password.length < 6) return NextResponse.json({ error: "Password must be at least 6 characters" }, { status: 400 });

    if (rateLimited(`signup-ip-${ip}`, 5, 3600)) {
      return NextResponse.json({ error: "Too many signup attempts." }, { status: 429 });
    }

    const existing = await db.user.findFirst({ where: { email }, select: { id: true } });
    if (existing) return NextResponse.json({ error: "An account with this email already exists." }, { status: 409 });

    const passwordHash = await hashPassword(password);
    const user = await db.user.create({
      data: { name, email, phone: phone || null, username: null, passwordHash, role: "STUDENT", isBanned: false, signupMethod: "credentials" },
    });

    await createSession(user.id);
    await audit({ actorId: user.id, action: "signup", entity: "User", entityId: user.id, ip });

    return NextResponse.json({
      ok: true,
      user: { id: user.id, name: user.name, email: user.email, phone: user.phone, role: user.role, username: user.username },
    });
  }

  // ─── GOOGLE SIGN-IN MODE ───
  if (b.mode === "google" || b.idToken) {
    const idToken = typeof b.idToken === "string" ? b.idToken : "";
    if (!idToken) return NextResponse.json({ error: "Missing Google ID token" }, { status: 400 });

    let googleUser: { email?: string; name?: string };
    try {
      const resp = await fetch(`https://oauth2.googleapis.com/tokeninfo?id_token=${encodeURIComponent(idToken)}`);
      if (!resp.ok) return NextResponse.json({ error: "Invalid Google token" }, { status: 401 });
      googleUser = await resp.json();
    } catch {
      return NextResponse.json({ error: "Could not verify Google token" }, { status: 502 });
    }

    const email = googleUser.email?.toLowerCase().trim();
    if (!email) return NextResponse.json({ error: "Google account has no email" }, { status: 400 });
    const name = googleUser.name || email.split("@")[0];

    let user = await db.user.findFirst({ where: { email } });
    if (!user) {
      const randomPassword = crypto.randomUUID() + crypto.randomUUID();
      const passwordHash = await hashPassword(randomPassword);
      user = await db.user.create({
        data: { email, name, passwordHash, role: "STUDENT", isVerified: true, isBanned: false, signupMethod: "google" },
      });
    } else if (user.isBanned) {
      return NextResponse.json({ error: "Account is suspended" }, { status: 403 });
    }

    await createSession(user.id);
    await audit({ actorId: user.id, action: "login_google", entity: "User", entityId: user.id, ip });

    return NextResponse.json({
      ok: true,
      user: { id: user.id, name: user.name, email: user.email, phone: user.phone, role: user.role, username: user.username },
    });
  }

  // ─── LOGIN MODE (original) ───
  const username = typeof b.username === "string" ? b.username.trim().toLowerCase().slice(0, 100) : "";
  const password = typeof b.password === "string" ? b.password.slice(0, 200) : "";

  if (!username || !password) {
    return NextResponse.json({ error: "Username and password are required" }, { status: 400 });
  }

  if (rateLimited(rateLimitKey("cred-ip", ip), 10, 3600)) {
    return NextResponse.json({ error: "Too many login attempts." }, { status: 429 });
  }
  if (rateLimited(rateLimitKey("cred-user", username), 10, 3600)) {
    return NextResponse.json({ error: "Too many login attempts." }, { status: 429 });
  }

  const user = await db.user.findFirst({ where: { OR: [{ username }, { email: username }] } });

  if (!user || !user.passwordHash) {
    return NextResponse.json({ error: "Invalid email or password" }, { status: 401 });
  }
  if (user.isBanned) {
    return NextResponse.json({ error: "Account is suspended." }, { status: 403 });
  }

  const valid = await verifyPassword(password, user.passwordHash);
  if (!valid) {
    await audit({ actorId: user.id, action: "login_failed", entity: "User", entityId: user.id, ip });
    return NextResponse.json({ error: "Invalid credentials" }, { status: 401 });
  }

  await createSession(user.id);
  await audit({ actorId: user.id, action: "login", entity: "User", entityId: user.id, ip });

  return NextResponse.json({
    ok: true,
    user: { id: user.id, name: user.name, email: user.email, phone: user.phone, role: user.role, username: user.username },
  });
}
