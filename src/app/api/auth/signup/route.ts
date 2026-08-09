/**
 * POST /api/auth/signup
 * Body: { mode, name, email, phone, password }
 *
 * Student signup with email + password (no OTP needed).
 * - Validates input
 * - Rate-limits per IP
 * - Checks email uniqueness
 * - Creates User (role=STUDENT) with passwordHash
 * - Creates Session, sets cookie
 * - Returns user credentials (same shape as /api/auth/credentials)
 */
import { NextRequest, NextResponse } from "next/server";
import { db } from "@/lib/db";
import { rateLimited } from "@/lib/rate-limit";
import { createSession } from "@/lib/session";
import { audit } from "@/lib/audit";
import { hashPassword } from "@/lib/password";
import { z } from "zod";

const schema = z.object({
  mode: z.string().default("student"),
  name: z.string().min(1).max(100),
  email: z.string().email().max(200),
  phone: z.string().max(30).optional().default(""),
  password: z.string().min(6).max(200),
});

export async function POST(req: NextRequest) {
  let body: unknown;
  try {
    body = await req.json();
  } catch {
    return NextResponse.json({ error: "Invalid JSON" }, { status: 400 });
  }

  const parsed = schema.safeParse(body);
  if (!parsed.success) {
    const msg = parsed.error.issues[0]?.message || "Invalid input";
    return NextResponse.json({ error: msg }, { status: 400 });
  }

  const { name, email, phone, password } = parsed.data;
  const emailLower = email.trim().toLowerCase();

  const ip = req.headers.get("x-forwarded-for")?.split(",")[0] || "unknown";

  // Rate limit: 5 signups per IP per hour
  if (rateLimited(`signup-ip-${ip}`, 5, 3600)) {
    return NextResponse.json(
      { error: "Too many signup attempts. Try again later." },
      { status: 429 },
    );
  }

  // Check if email already exists
  const existing = await db.user.findFirst({
    where: { email: emailLower },
    select: { id: true },
  });
  if (existing) {
    return NextResponse.json(
      { error: "An account with this email already exists. Please log in." },
      { status: 409 },
    );
  }

  // Hash password
  const passwordHash = await hashPassword(password);

  // Create user (phone is unique — only set if non-empty to avoid constraint conflicts)
  const user = await db.user.create({
    data: {
      name: name.trim(),
      email: emailLower,
      phone: phone?.trim() || null,
      username: null,  // students don't use username login
      passwordHash,
      role: "STUDENT",
      isBanned: false,
      signupMethod: "credentials",
    },
  });

  // Create session
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
      username: user.username,
    },
  });
}
