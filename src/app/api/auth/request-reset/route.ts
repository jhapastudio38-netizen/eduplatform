/**
 * POST /api/auth/request-reset
 * Body: { email: string }
 *
 * Sends a 6-digit reset code to the user's email if an account exists.
 * Always returns 200 OK (we don't leak whether the email exists or not).
 *
 * The OTP is stored in the OtpCode table with purpose="reset" and a 15-min
 * expiry. The user then calls POST /api/auth/reset-password with the code.
 */
import { NextRequest, NextResponse } from "next/server";
import { db } from "@/lib/db";
import { generateOtp, rateLimitKey } from "@/lib/security";
import { rateLimited } from "@/lib/rate-limit";
import { z } from "zod";

const schema = z.object({
  email: z.string().trim().toLowerCase().email("Invalid email"),
});

export async function POST(req: NextRequest) {
  const ip = req.headers.get("x-forwarded-for")?.split(",")[0] || "unknown";
  if (rateLimited(rateLimitKey("reset-req-ip", ip), 10, 3600)) {
    return NextResponse.json({ error: "Too many requests. Try again later." }, { status: 429 });
  }

  const body = await req.json().catch(() => null);
  const parsed = schema.safeParse(body);
  if (!parsed.success) {
    return NextResponse.json({ error: parsed.error.issues[0]?.message ?? "Invalid email" }, { status: 400 });
  }
  const email = parsed.data.email;

  // Per-email rate limit — 3 reset requests per hour
  if (rateLimited(rateLimitKey("reset-req-email", email), 3, 3600)) {
    return NextResponse.json({ ok: true }); // Don't leak rate limit
  }

  // Look up the user — but ALWAYS return ok so we don't leak which emails exist
  const user = await db.user.findUnique({ where: { email } });

  if (user) {
    // Generate + store a 6-digit reset code
    const code = generateOtp(6);
    await db.otpCode.create({
      data: {
        contact: email,
        code,
        purpose: "reset",
        expiresAt: new Date(Date.now() + 15 * 60 * 1000),
      },
    });

    // Send the email — uses the existing OTP email sender (Gmail SMTP)
    try {
      const { sendOtpEmail } = await import("@/lib/otp");
      await sendOtpEmail(email, code, "reset");
    } catch (e) {
      console.error("Reset email send failed:", e);
      // Still return ok — caller can retry
    }
  }

  return NextResponse.json({ ok: true, message: "If an account exists with that email, a reset code has been sent." });
}
