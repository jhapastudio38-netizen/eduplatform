/**
 * POST /api/auth/verify-otp
 * Body: { contact: string, code: string, role: "STUDENT"|"TEACHER"|"ADMIN", name?: string }
 *
 * - Validates input
 * - Rate-limits verification attempts per contact
 * - Looks up the most recent non-consumed OTP for the contact
 * - Compares hash (constant-time)
 * - Creates / updates User, creates Session, sets cookie
 */
import { NextRequest, NextResponse } from "next/server";
import { db } from "@/lib/db";
import { CONFIG } from "@/lib/config";
import {
  contactSchema,
  emailSchema,
  otpSchema,
  phoneSchema,
  rateLimitKey,
  roleSchema,
  sha256,
  constantTimeEqual,
} from "@/lib/security";
import { rateLimited } from "@/lib/rate-limit";
import { createSession } from "@/lib/session";
import { audit } from "@/lib/audit";

export async function POST(req: NextRequest) {
  let body: unknown;
  try {
    body = await req.json();
  } catch {
    return NextResponse.json({ error: "Invalid JSON" }, { status: 400 });
  }
  const b = body as {
    contact?: unknown;
    code?: unknown;
    role?: unknown;
    name?: unknown;
  };

  const contact = contactSchema.safeParse(b.contact);
  const code = otpSchema.safeParse(b.code);
  const role = roleSchema.safeParse(b.role);
  const name =
    typeof b.name === "string" ? b.name.trim().slice(0, 100) : undefined;

  if (!contact.success) return NextResponse.json({ error: "Invalid contact" }, { status: 400 });
  if (!code.success) return NextResponse.json({ error: "Invalid code" }, { status: 400 });
  if (!role.success) return NextResponse.json({ error: "Invalid role" }, { status: 400 });

  const ip = req.headers.get("x-forwarded-for")?.split(",")[0] || "unknown";

  // Rate limit verification attempts (10/hour per contact)
  if (rateLimited(rateLimitKey("verify", contact.data), 10, 3600)) {
    return NextResponse.json(
      { error: "Too many attempts. Try again later." },
      { status: 429 },
    );
  }

  // Fetch latest OTP for this contact
  const otp = await db.otpCode.findFirst({
    where: { contact: contact.data, consumed: false },
    orderBy: { createdAt: "desc" },
  });

  if (!otp || otp.expiresAt < new Date()) {
    return NextResponse.json({ error: "Code expired. Request a new one." }, { status: 410 });
  }

  if (otp.attempts >= CONFIG.auth.otpMaxAttempts) {
    await db.otpCode.update({ where: { id: otp.id }, data: { consumed: true } });
    return NextResponse.json({ error: "Too many wrong attempts. Request a new code." }, { status: 429 });
  }

  // Constant-time compare
  const submittedHash = sha256(code.data);
  if (!constantTimeEqual(submittedHash, otp.code)) {
    await db.otpCode.update({
      where: { id: otp.id },
      data: { attempts: { increment: 1 } },
    });
    return NextResponse.json({ error: "Wrong code" }, { status: 401 });
  }

  // Mark consumed
  await db.otpCode.update({ where: { id: otp.id }, data: { consumed: true } });

  // Create or fetch user. Email/phone are unique — try both.
  const isEmail = emailSchema.safeParse(contact.data).success;
  const isPhone = phoneSchema.safeParse(contact.data).success;

  let user = await db.user.findFirst({
    where: {
      OR: [
        isEmail ? { email: contact.data } : {},
        isPhone ? { phone: contact.data } : {},
      ].filter((c) => Object.keys(c).length > 0),
    },
  });

  if (!user) {
    // First-time login → create account
    user = await db.user.create({
    data: {
        email: isEmail ? contact.data : `phone-${contact.data}@placeholder.local`,
        phone: isPhone ? contact.data : null,
        name: name || (isEmail ? contact.data.split("@")[0] : "User"),
        role: role.data,
        isVerified: true,
      },
    });
  } else {
    // Existing user — keep their existing role (security: cannot self-escalate)
    // but allow admins to retain admin even if they picked "STUDENT" on the form.
    user = await db.user.update({
      where: { id: user.id },
      data: {
        isVerified: true,
        ...(isEmail && !user.email ? { email: contact.data } : {}),
        ...(isPhone && !user.phone ? { phone: contact.data } : {}),
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
