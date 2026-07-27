/**
 * POST /api/auth/request-reset
 * Body: { email: string }
 * Sends a 6-digit reset code to the user's email if an account exists.
 */
import { NextRequest, NextResponse } from "next/server";
import { db } from "@/lib/db";
import { generateOtp, rateLimitKey } from "@/lib/security";
import { rateLimited } from "@/lib/rate-limit";
import { z } from "zod";

const schema = z.object({ email: z.string().trim().toLowerCase().email("Invalid email") });

export async function POST(req: NextRequest) {
  const ip = req.headers.get("x-forwarded-for")?.split(",")[0] || "unknown";
  if (rateLimited(rateLimitKey("reset-req-ip", ip), 10, 3600)) {
    return NextResponse.json({ error: "Too many requests." }, { status: 429 });
  }
  const body = await req.json().catch(() => null);
  const parsed = schema.safeParse(body);
  if (!parsed.success) return NextResponse.json({ error: parsed.error.issues[0]?.message ?? "Invalid email" }, { status: 400 });
  const email = parsed.data.email;

  if (rateLimited(rateLimitKey("reset-req-email", email), 3, 3600)) {
    return NextResponse.json({ ok: true });
  }

  const user = await db.user.findUnique({ where: { email } });
  if (user) {
    const code = generateOtp(6);
    await db.otpCode.create({ data: { contact: email, code, purpose: "reset", expiresAt: new Date(Date.now() + 15 * 60 * 1000) } });
    try {
      const { sendOtpEmail } = await import("@/lib/otp");
      await sendOtpEmail(email, code, "reset");
    } catch (e) { console.error("Reset email send failed:", e); }
  }
  return NextResponse.json({ ok: true, message: "If an account exists with that email, a reset code has been sent." });
}
