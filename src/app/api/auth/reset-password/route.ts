/**
 * POST /api/auth/reset-password
 * Body: { email: string, code: string, newPassword: string }
 *
 * Verifies the 6-digit reset code against the OtpCode table (purpose="reset").
 * If valid + not expired + not consumed, sets the user's passwordHash to the
 * new password and marks the OTP as consumed.
 *
 * Returns 200 on success, 400 on invalid/expired code, 429 on too many attempts.
 */
import { NextRequest, NextResponse } from "next/server";
import { db } from "@/lib/db";
import { hashPassword } from "@/lib/password";
import { rateLimitKey, otpSchema } from "@/lib/security";
import { rateLimited } from "@/lib/rate-limit";
import { audit } from "@/lib/audit";
import { z } from "zod";

const schema = z.object({
  email: z.string().trim().toLowerCase().email("Invalid email"),
  code: otpSchema,
  newPassword: z.string().min(6, "Password must be at least 6 characters").max(200),
});

export async function POST(req: NextRequest) {
  const ip = req.headers.get("x-forwarded-for")?.split(",")[0] || "unknown";
  if (rateLimited(rateLimitKey("reset-ip", ip), 10, 3600)) {
    return NextResponse.json({ error: "Too many attempts. Try again later." }, { status: 429 });
  }

  const body = await req.json().catch(() => null);
  const parsed = schema.safeParse(body);
  if (!parsed.success) {
    return NextResponse.json({ error: parsed.error.issues[0]?.message ?? "Invalid data" }, { status: 400 });
  }
  const { email, code, newPassword } = parsed.data;

  // Look up the most recent unused reset OTP for this email
  const otp = await db.otpCode.findFirst({
    where: {
      contact: email,
      purpose: "reset",
      consumed: false,
      expiresAt: { gt: new Date() },
    },
    orderBy: { createdAt: "desc" },
  });

  if (!otp || otp.code !== code) {
    // Increment attempts on the OTP if it exists
    if (otp) {
      await db.otpCode.update({
        where: { id: otp.id },
        data: { attempts: { increment: 1 } },
      });
      if (otp.attempts >= 5) {
        await db.otpCode.update({
          where: { id: otp.id },
          data: { consumed: true },
        });
      }
    }
    return NextResponse.json({ error: "Invalid or expired code" }, { status: 400 });
  }

  // Find the user
  const user = await db.user.findUnique({ where: { email } });
  if (!user) {
    return NextResponse.json({ error: "Account not found" }, { status: 404 });
  }

  // Hash + save the new password
  const passwordHash = await hashPassword(newPassword);
  await db.user.update({
    where: { id: user.id },
    data: { passwordHash },
  });

  // Mark the OTP as consumed so it can't be reused
  await db.otpCode.update({
    where: { id: otp.id },
    data: { consumed: true },
  });

  await audit({
    actorId: user.id,
    action: "reset_password",
    entity: "User",
    entityId: user.id,
    ip,
  });

  return NextResponse.json({ ok: true });
}
