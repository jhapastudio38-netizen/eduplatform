/**
 * POST /api/auth/reset-password
 * Body: { email, code, newPassword }
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
    return NextResponse.json({ error: "Too many attempts." }, { status: 429 });
  }
  const body = await req.json().catch(() => null);
  const parsed = schema.safeParse(body);
  if (!parsed.success) return NextResponse.json({ error: parsed.error.issues[0]?.message ?? "Invalid data" }, { status: 400 });
  const { email, code, newPassword } = parsed.data;

  const otp = await db.otpCode.findFirst({
    where: { contact: email, purpose: "reset", consumed: false, expiresAt: { gt: new Date() } },
    orderBy: { createdAt: "desc" },
  });

  if (!otp || otp.code !== code) {
    if (otp) {
      await db.otpCode.update({ where: { id: otp.id }, data: { attempts: { increment: 1 } } });
      if (otp.attempts >= 5) await db.otpCode.update({ where: { id: otp.id }, data: { consumed: true } });
    }
    return NextResponse.json({ error: "Invalid or expired code" }, { status: 400 });
  }

  const user = await db.user.findUnique({ where: { email } });
  if (!user) return NextResponse.json({ error: "Account not found" }, { status: 404 });

  const passwordHash = await hashPassword(newPassword);
  await db.user.update({ where: { id: user.id }, data: { passwordHash } });
  await db.otpCode.update({ where: { id: otp.id }, data: { consumed: true } });
  await audit({ actorId: user.id, action: "reset_password", entity: "User", entityId: user.id, ip });

  return NextResponse.json({ ok: true });
}
