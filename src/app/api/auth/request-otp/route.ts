/**
 * POST /api/auth/request-otp
 * Body: { contact: string }   // email or phone
 *
 * - Validates contact
 * - Rate-limits per IP + per contact
 * - Generates 6-digit OTP, stores hash + expiry
 * - Sends via Resend (email) or SMS (phone)
 */
import { NextRequest, NextResponse } from "next/server";
import { db } from "@/lib/db";
import { CONFIG } from "@/lib/config";
import {
  contactSchema,
  emailSchema,
  generateOtp,
  phoneSchema,
  rateLimitKey,
  sha256,
} from "@/lib/security";
import { rateLimited } from "@/lib/rate-limit";
import { sendOtpEmail, sendOtpPhone } from "@/lib/otp";

export async function POST(req: NextRequest) {
  let body: unknown;
  try {
    body = await req.json();
  } catch {
    return NextResponse.json({ error: "Invalid JSON" }, { status: 400 });
  }

  const parsed = contactSchema.safeParse((body as { contact?: unknown }).contact);
  if (!parsed.success) {
    return NextResponse.json(
      { error: parsed.error.issues[0]?.message ?? "Invalid contact" },
      { status: 400 },
    );
  }
  const contact = parsed.data;
  const isEmail = emailSchema.safeParse(contact).success;
  const isPhone = phoneSchema.safeParse(contact).success;

  // ─── Rate limit: per IP (5 req/hour) + per contact (5 req/hour) ──────────
  const ip = req.headers.get("x-forwarded-for")?.split(",")[0] || "unknown";
  if (rateLimited(rateLimitKey("otp-ip", ip), CONFIG.auth.rateLimit.otpRequestPerHour, 3600)) {
    return NextResponse.json(
      { error: "Too many requests from this IP. Try again later." },
      { status: 429 },
    );
  }
  if (rateLimited(rateLimitKey("otp-contact", contact), CONFIG.auth.rateLimit.otpRequestPerHour, 3600)) {
    return NextResponse.json(
      { error: "Too many OTP requests. Try again later." },
      { status: 429 },
    );
  }

  // ─── Generate & store OTP ────────────────────────────────────────────────
  const code = generateOtp(6);
  const expiresAt = new Date(Date.now() + CONFIG.auth.otpTtlMinutes * 60 * 1000);

  // Invalidate previous unconsumed OTPs for this contact
  await db.otpCode.updateMany({
    where: { contact, consumed: false },
    data: { consumed: true },
  });

  await db.otpCode.create({
    data: {
      contact,
      code: sha256(code), // store hash only
      expiresAt,
      purpose: "login",
    },
  });

  // ─── Deliver ─────────────────────────────────────────────────────────────
  let delivery;
  if (isEmail) {
    delivery = await sendOtpEmail(contact, code);
  } else if (isPhone) {
    delivery = await sendOtpPhone(contact, code);
  } else {
    return NextResponse.json({ error: "Unsupported contact" }, { status: 400 });
  }

  if (!delivery.ok) {
    return NextResponse.json(
      { error: `Failed to send OTP: ${delivery.message}` },
      { status: 502 },
    );
  }

  return NextResponse.json({
    ok: true,
    channel: delivery.channel,
  });
}
