/**
 * POST /api/auth/signup
 * Body: { name: string, email: string, phone?: string, password: string }
 *
 * Traditional email + password signup for students (no OTP).
 *
 * Flow:
 *   1. Validate input (name, email, password ≥ 6 chars)
 *   2. Rate-limit per IP
 *   3. Reject if email already registered with a password
 *   4. Hash password (scrypt)
 *   5. Create User (role STUDENT, signupMethod "credentials")
 *   6. Create Session, set ep_sid cookie
 *   7. Return { ok, user }
 *
 * Returns:
 *   200 { ok: true, user } — account created + logged in
 *   400 — invalid input
 *   409 — email already registered
 *   429 — rate limited
 */
import { NextRequest, NextResponse } from "next/server";
import { db } from "@/lib/db";
import { rateLimitKey, emailSchema, phoneSchema } from "@/lib/security";
import { rateLimited } from "@/lib/rate-limit";
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
  const b = body as {
    name?: unknown;
    email?: unknown;
    phone?: unknown;
    password?: unknown;
  };

  const name = typeof b.name === "string" ? b.name.trim().slice(0, 100) : "";
  const emailRaw = typeof b.email === "string" ? b.email.trim().toLowerCase() : "";
  const phoneRaw = typeof b.phone === "string" ? b.phone.trim() : "";
  const password = typeof b.password === "string" ? b.password : "";

  const emailParsed = emailSchema.safeParse(emailRaw);
  if (!name) {
    return NextResponse.json({ error: "Name is required" }, { status: 400 });
  }
  if (!emailParsed.success) {
    return NextResponse.json({ error: "Enter a valid email" }, { status: 400 });
  }
  if (password.length < 6) {
    return NextResponse.json(
      { error: "Password must be at least 6 characters" },
      { status: 400 }
    );
  }
  const phone =
    phoneRaw && phoneSchema.safeParse(phoneRaw).success ? phoneRaw : null;

  const ip = req.headers.get("x-forwarded-for")?.split(",")[0] || "unknown";

  // Rate limit: 5 signups per IP per hour
  if (rateLimited(rateLimitKey("signup-ip", ip), 5, 3600)) {
    return NextResponse.json(
      { error: "Too many signup attempts. Try again later." },
      { status: 429 }
    );
  }

  // Check if email is already registered
  const existing = await db.user.findUnique({ where: { email: emailParsed.data } });
  if (existing) {
    return NextResponse.json(
      { error: "This email is already registered. Please sign in." },
      { status: 409 }
    );
  }

  // Hash password
  const passwordHash = await hashPassword(password);

  // Create user
  const user = await db.user.create({
    data: {
      email: emailParsed.data,
      phone,
      name,
      role: "STUDENT",
      isVerified: true, // email-based signup — verified by virtue of having the email
      signupMethod: "credentials",
      passwordHash,
    },
  });

  await createSession(user.id);
  await audit({
    actorId: user.id,
    action: "signup",
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
