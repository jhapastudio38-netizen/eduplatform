/**
 * POST /api/auth/set-password
 * Body: { password: string }
 *
 * Sets a password for the currently logged-in user (via session cookie).
 * Used after OTP signup when the student chooses to set a password.
 *
 * Returns:
 *   200 { ok: true } — password set successfully
 *   401 — not logged in
 *   400 — invalid password (too short)
 *   429 — rate limited
 */
import { NextRequest, NextResponse } from "next/server";
import { getCurrentUser } from "@/lib/session";
import { db } from "@/lib/db";
import { hashPassword } from "@/lib/password";
import { rateLimited } from "@/lib/rate-limit";
import { rateLimitKey } from "@/lib/security";
import { audit } from "@/lib/audit";
import { z } from "zod";

const schema = z.object({
  password: z.string().min(6, "Password must be at least 6 characters").max(200),
});

export async function POST(req: NextRequest) {
  const user = await getCurrentUser();
  if (!user) {
    return NextResponse.json({ error: "Not logged in" }, { status: 401 });
  }

  // Rate limit: 5 password sets per hour
  const ip = req.headers.get("x-forwarded-for")?.split(",")[0] || "unknown";
  if (rateLimited(rateLimitKey("set-password", user.id), 5, 3600)) {
    return NextResponse.json({ error: "Too many attempts. Try again later." }, { status: 429 });
  }

  const body = await req.json().catch(() => null);
  const parsed = schema.safeParse(body);
  if (!parsed.success) {
    return NextResponse.json(
      { error: parsed.error.issues[0]?.message ?? "Invalid password" },
      { status: 400 }
    );
  }

  const passwordHash = await hashPassword(parsed.data.password);

  await db.user.update({
    where: { id: user.id },
    data: { passwordHash },
  });

  await audit({
    actorId: user.id,
    action: "set_password",
    entity: "User",
    entityId: user.id,
    ip,
  });

  return NextResponse.json({ ok: true });
}
