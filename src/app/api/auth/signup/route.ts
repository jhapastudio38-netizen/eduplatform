import { NextRequest, NextResponse } from "next/server";
import { db } from "@/lib/db";
import { rateLimited } from "@/lib/rate-limit";
import { createSession } from "@/lib/session";
import { audit } from "@/lib/audit";
import { hashPassword } from "@/lib/password";
import { z } from "zod";

const schema = z.object({
  name: z.string().min(1).max(100),
  email: z.string().email().max(200),
  phone: z.string().max(30).optional().default(""),
  password: z.string().min(6).max(200),
});

export async function POST(req: NextRequest) {
  let body: unknown;
  try { body = await req.json(); } catch { return NextResponse.json({ error: "Invalid JSON" }, { status: 400 }); }

  const parsed = schema.safeParse(body);
  if (!parsed.success) {
    return NextResponse.json({ error: parsed.error.issues[0]?.message || "Invalid input" }, { status: 400 });
  }

  const { name, email, phone, password } = parsed.data;
  const emailLower = email.trim().toLowerCase();
  const ip = req.headers.get("x-forwarded-for")?.split(",")[0] || "unknown";

  if (rateLimited(`signup-ip-${ip}`, 5, 3600)) {
    return NextResponse.json({ error: "Too many signup attempts." }, { status: 429 });
  }

  const existing = await db.user.findFirst({ where: { email: emailLower }, select: { id: true } });
  if (existing) {
    return NextResponse.json({ error: "An account with this email already exists." }, { status: 409 });
  }

  const passwordHash = await hashPassword(password);
  const user = await db.user.create({
    data: { name: name.trim(), email: emailLower, phone: phone?.trim() || null, username: null, passwordHash, role: "STUDENT", isBanned: false, signupMethod: "credentials" },
  });

  await createSession(user.id);
  await audit({ actorId: user.id, action: "signup", entity: "User", entityId: user.id, ip });

  return NextResponse.json({
    ok: true,
    user: { id: user.id, name: user.name, email: user.email, phone: user.phone, role: user.role, username: user.username },
  });
}
