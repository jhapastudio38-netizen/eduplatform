/**
 * POST /api/auth/verify-otp
 * Body: { contact: string, code: string, role: "STUDENT"|"TEACHER"|"ADMIN", name?: string, email?: string, phone?: string }
 *
 * - Validates input
 * - Rate-limits verification attempts per contact
 * - Looks up the most recent non-consumed OTP for the contact
 * - Compares hash (constant-time)
 * - Creates / updates User with name + email + phone, creates Session, sets cookie
 *
 * Students provide: contact (for OTP) + name + phone (if OTP was email) OR + email (if OTP was phone)
 * Teachers/admins should NOT use this route — they use /api/auth/credentials
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
import { hashPassword } from "@/lib/password";

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
    email?: unknown;
    phone?: unknown;
    password?: unknown; // optional: set password after OTP
  };

  const contact = contactSchema.safeParse(b.contact);
  const code = otpSchema.safeParse(b.code);
  const role = roleSchema.safeParse(b.role);
  const name =
    typeof b.name === "string" ? b.name.trim().slice(0, 100) : undefined;
  // Optional supplementary contact fields (so we can store both email AND phone for students)
  const emailRaw = typeof b.email === "string" ? b.email.trim().toLowerCase() : undefined;
  const phoneRaw = typeof b.phone === "string" ? b.phone.trim() : undefined;
  const emailSupp = emailRaw && emailSchema.safeParse(emailRaw).success ? emailRaw : undefined;
  const phoneSupp = phoneRaw && phoneSchema.safeParse(phoneRaw).success ? phoneRaw : undefined;
  // Optional password to set after OTP verification
  const passwordRaw = typeof b.password === "string" ? b.password : "";

  if (!contact.success) return NextResponse.json({ error: "Invalid contact" }, { status: 400 });
  if (!code.success) return NextResponse.json({ error: "Invalid code" }, { status: 400 });
  if (!role.success) return NextResponse.json({ error: "Invalid role" }, { status: 400 });
  // Students MUST provide their name
  if (role.data === "STUDENT" && !name) {
    return NextResponse.json({ error: "Please enter your name" }, { status: 400 });
  }

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

  // Students MUST provide a phone number (either as OTP contact, or as supplementary)
  if (role.data === "STUDENT") {
    const hasPhone = isPhone || !!phoneSupp;
    if (!hasPhone) {
      return NextResponse.json({ error: "Phone number is required" }, { status: 400 });
    }
  }

  let user = await db.user.findFirst({
    where: {
      OR: [
        isEmail ? { email: contact.data } : {},
        isPhone ? { phone: contact.data } : {},
        emailSupp ? { email: emailSupp } : {},
        phoneSupp ? { phone: phoneSupp } : {},
      ].filter((c) => Object.keys(c).length > 0),
    },
  });

  if (!user) {
    // First-time login → create account
    // Resolve final email + phone: prefer supplementary, fall back to contact
    const finalEmail = emailSupp ?? (isEmail ? contact.data : `phone-${phoneSupp ?? contact.data}@placeholder.local`);
    const finalPhone = phoneSupp ?? (isPhone ? contact.data : null);
    // Hash password if provided
    const passwordHash = passwordRaw && passwordRaw.length >= 6 ? await hashPassword(passwordRaw) : undefined;
    user = await db.user.create({
      data: {
        email: finalEmail,
        phone: finalPhone,
        name: name || (isEmail ? contact.data.split("@")[0] : "Student"),
        role: role.data,
        isVerified: true,
        signupMethod: isEmail ? "otp_email" : "otp_phone",
        ...(passwordHash ? { passwordHash } : {}),
      },
    });
  } else {
    // Existing user — keep their existing role (security: cannot self-escalate)
    // If password provided and user doesn't have one, set it
    const shouldSetPassword = passwordRaw && passwordRaw.length >= 6 && !user.passwordHash;
    const passwordHash = shouldSetPassword ? await hashPassword(passwordRaw) : undefined;
    user = await db.user.update({
      where: { id: user.id },
      data: {
        isVerified: true,
        name: name ?? user.name,
        ...(emailSupp && !user.email?.includes("@placeholder.local") ? {} : (emailSupp ? { email: emailSupp } : {})),
        ...(isEmail && !user.email ? { email: contact.data } : {}),
        ...(phoneSupp && !user.phone ? { phone: phoneSupp } : {}),
        ...(isPhone && !user.phone ? { phone: contact.data } : {}),
        ...(passwordHash ? { passwordHash } : {}),
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
