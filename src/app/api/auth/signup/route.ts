/**
 * POST /api/auth/signup
 *
 * Two signup modes, selected by the `mode` field:
 *
 *   1. Teacher signup with invite code:
 *      { mode: "teacher", name, email, username, password, inviteCode, phone? }
 *      - Looks up a non-expired, non-revoked TeacherInvite by code
 *      - Creates a TEACHER user with passwordHash
 *      - Marks the invite as consumed
 *      - Creates a session, sets cookie
 *
 *   2. Student signup with email + password (no OTP required):
 *      { mode: "student", name, email, password, phone? }
 *      - Creates a STUDENT user with passwordHash + signupMethod="credentials"
 *      - Creates a session, sets cookie
 *
 * Security:
 *   - Input validated with Zod
 *   - Rate-limited per IP (5 signups per hour) and per email (3 per hour)
 *   - Username normalized to lowercase
 *   - Password hashed with scrypt
 *   - Audit-logged
 */
import { NextRequest, NextResponse } from "next/server";
import { db } from "@/lib/db";
import {
  emailSchema,
  phoneSchema,
  rateLimitKey,
} from "@/lib/security";
import { rateLimited } from "@/lib/rate-limit";
import { createSession } from "@/lib/session";
import { audit } from "@/lib/audit";
import { hashPassword } from "@/lib/password";

const USERNAME_RE = /^[a-z0-9._-]+$/;
const PASSWORD_MIN = 6;
const PASSWORD_MAX = 200;

export async function POST(req: NextRequest) {
  const ip = req.headers.get("x-forwarded-for")?.split(",")[0] || "unknown";

  // Global rate limit — 5 signups per IP per hour
  if (rateLimited(rateLimitKey("signup-ip", ip), 5, 3600)) {
    return NextResponse.json(
      { error: "Too many signups from this IP. Try again later." },
      { status: 429 },
    );
  }

  let body: unknown;
  try {
    body = await req.json();
  } catch {
    return NextResponse.json({ error: "Invalid JSON" }, { status: 400 });
  }
  const b = body as {
    mode?: unknown;
    name?: unknown;
    email?: unknown;
    username?: unknown;
    password?: unknown;
    phone?: unknown;
    inviteCode?: unknown;
  };

  const mode = typeof b.mode === "string" ? b.mode : "";
  if (mode !== "teacher" && mode !== "student") {
    return NextResponse.json({ error: "Invalid mode — must be 'teacher' or 'student'" }, { status: 400 });
  }

  const name = typeof b.name === "string" ? b.name.trim().slice(0, 100) : "";
  const emailRaw = typeof b.email === "string" ? b.email.trim().toLowerCase() : "";
  const password = typeof b.password === "string" ? b.password : "";
  const phoneRaw = typeof b.phone === "string" ? b.phone.trim() : "";
  const username =
    typeof b.username === "string" ? b.username.trim().toLowerCase().slice(0, 50) : "";
  const inviteCode =
    typeof b.inviteCode === "string" ? b.inviteCode.trim().toUpperCase() : "";

  if (!name) return NextResponse.json({ error: "Name is required" }, { status: 400 });
  if (!emailSchema.safeParse(emailRaw).success) {
    return NextResponse.json({ error: "Valid email is required" }, { status: 400 });
  }
  if (password.length < PASSWORD_MIN || password.length > PASSWORD_MAX) {
    return NextResponse.json(
      { error: `Password must be ${PASSWORD_MIN}-${PASSWORD_MAX} characters` },
      { status: 400 },
    );
  }
  const phone = phoneRaw && phoneSchema.safeParse(phoneRaw).success ? phoneRaw : null;

  // Per-email rate limit — 3 per hour
  if (rateLimited(rateLimitKey("signup-email", emailRaw), 3, 3600)) {
    return NextResponse.json(
      { error: "Too many signup attempts for this email. Try again later." },
      { status: 429 },
    );
  }

  // Check for existing user by email (and username for teacher mode)
  const orClauses: { email?: string; username?: string }[] = [{ email: emailRaw }];
  if (mode === "teacher" && username) orClauses.push({ username });
  const existing = await db.user.findFirst({ where: { OR: orClauses } });
  if (existing) {
    return NextResponse.json(
      { error: existing.email === emailRaw ? "Email already registered" : "Username already taken" },
      { status: 409 },
    );
  }

  // TEACHER MODE — validate invite code
  let invite: { id: string; presetEmail: string | null; expiresAt: Date; consumedBy: string | null; isRevoked: boolean } | null = null;
  if (mode === "teacher") {
    if (!username || username.length < 3 || !USERNAME_RE.test(username)) {
      return NextResponse.json(
        { error: "Username must be 3+ chars, lowercase letters/digits/._- only" },
        { status: 400 },
      );
    }
    if (!inviteCode) {
      return NextResponse.json({ error: "Invite code is required for teacher signup" }, { status: 400 });
    }
    invite = await db.teacherInvite.findUnique({ where: { code: inviteCode } });
    if (!invite || invite.isRevoked) {
      return NextResponse.json({ error: "Invalid or revoked invite code" }, { status: 403 });
    }
    if (invite.expiresAt < new Date()) {
      return NextResponse.json({ error: "Invite code has expired" }, { status: 403 });
    }
    if (invite.consumedBy) {
      return NextResponse.json({ error: "Invite code has already been used" }, { status: 403 });
    }
    // If the invite has a preset email and the signup email doesn't match, refuse
    if (invite.presetEmail && invite.presetEmail.toLowerCase() !== emailRaw) {
      return NextResponse.json(
        { error: "This invite code is tied to a different email. Contact the admin." },
        { status: 403 },
      );
    }
  }

  // Hash the password
  const passwordHash = await hashPassword(password);

  // Create the user
  const user = await db.user.create({
    data: {
      name,
      email: emailRaw,
      phone,
      role: mode === "teacher" ? "TEACHER" : "STUDENT",
      isVerified: true,
      passwordHash,
      username: mode === "teacher" ? username : null,
      signupMethod: "credentials",
    },
    select: {
      id: true,
      name: true,
      email: true,
      phone: true,
      role: true,
      username: true,
    },
  });

  // Mark the invite as consumed
  if (mode === "teacher" && invite) {
    await db.teacherInvite.update({
      where: { id: invite.id },
      data: {
        consumedBy: user.id,
        consumedAt: new Date(),
      },
    });
  }

  await createSession(user.id);
  await audit({
    actorId: user.id,
    action: mode === "teacher" ? "signup_teacher" : "signup_student",
    entity: "User",
    entityId: user.id,
    ip,
    metadata: { email: emailRaw, mode },
  });

  return NextResponse.json({ ok: true, user });
}
