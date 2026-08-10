/**
 * POST /api/auth/signup
 * Body: { name: string, email: string, phone?: string, password: string }
 *
 * Student signup with email + password (no OTP needed).
 * - Validates input
 * - Rate-limits per IP
 * - Checks if email already registered
 * - Creates User with passwordHash
 * - Creates Session, sets cookie
 * - Returns user + ok: true
 */
import { NextRequest, NextResponse } from "next/server";
import { db } from "@/lib/db";
import { rateLimitKey } from "@/lib/security";
import { rateLimited } from "@/lib/rate-limit";
import { createSession } from "@/lib/session";
import { hashPassword } from "@/lib/password";

export async function POST(req: NextRequest) {
  let body: unknown;
  try {
    body = await req.json();
  } catch {
    return NextResponse.json({ error: "Invalid JSON" }, { status: 400 });
  }

  const b = body as { name?: unknown; email?: unknown; phone?: unknown; password?: unknown };

  const name = typeof b.name === "string" ? b.name.trim().slice(0, 100) : "";
  const email = typeof b.email === "string" ? b.email.trim().toLowerCase().slice(0, 200) : "";
  const phone = typeof b.phone === "string" ? b.phone.trim().slice(0, 30) : "";
  const password = typeof b.password === "string" ? b.password : "";

  if (!name || !email || !password) {
    return NextResponse.json({ error: "Name, email, and password are required" }, { status: 400 });
  }
  if (!email.includes("@") || !email.includes(".")) {
    return NextResponse.json({ error: "Invalid email address" }, { status: 400 });
  }
  if (password.length < 6) {
    return NextResponse.json({ error: "Password must be at least 6 characters" }, { status: 400 });
  }

  const ip = req.headers.get("x-forwarded-for")?.split(",")[0] || "unknown";

  // Rate limit: 5 signups per IP per hour
  if (rateLimited(rateLimitKey("signup-ip", ip), 5, 3600)) {
    return NextResponse.json(
      { error: "Too many signup attempts from this IP. Try again later." },
      { status: 429 },
    );
  }

  // Check if email already registered
  const existing = await db.user.findFirst({ where: { email } });
  if (existing) {
    return NextResponse.json({ error: "Email already registered. Please sign in." }, { status: 409 });
  }

  // Generate username from email
  const baseUsername = email.split("@")[0];
  let username = baseUsername;
  let suffix = 1;
  while (await db.user.findFirst({ where: { username } })) {
    username = `${baseUsername}${suffix++}`;
  }

  // Hash password
  const passwordHash = await hashPassword(password);

  // Create user
  const user = await db.user.create({
    data: {
      name,
      email,
      phone: phone || null,
      username,
      role: "STUDENT",
      passwordHash,
      isVerified: true,
      signupMethod: "credentials",
    },
  });

  // Create session
  await createSession(user.id);

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
