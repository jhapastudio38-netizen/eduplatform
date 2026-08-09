/**
 * POST /api/auth/signup
 * Body: { name, email, phone, password }
 *
 * Traditional signup — creates a student account with email + password.
 */
import { NextRequest, NextResponse } from "next/server";
import { db } from "@/lib/db";
import { rateLimited } from "@/lib/rate-limit";
import { createSession } from "@/lib/session";
import { audit } from "@/lib/audit";
import { hashPassword } from "@/lib/password";

export async function POST(req: NextRequest) {
  let body: unknown;
  try { body = await req.json(); } catch { return NextResponse.json({ error: "Invalid JSON" }, { status: 400 }); }
  const b = body as Record<string, unknown>;

  const name = typeof b.name === "string" ? b.name.trim().slice(0, 100) : "";
  const email = typeof b.email === "string" ? b.email.trim().toLowerCase().slice(0, 200) : "";
  const phone = typeof b.phone === "string" ? b.phone.trim().slice(0, 30) : "";
  const password = typeof b.password === "string" ? b.password.slice(0, 200) : "";

  if (!name) return NextResponse.json({ error: "Name is required" }, { status: 400 });
  if (!email || !email.includes("@")) return NextResponse.json({ error: "Valid email is required" }, { status: 400 });
  if (password.length < 6) return NextResponse.json({ error: "Password must be at least 6 characters" }, { status: 400 });

  const ip = req.headers.get("x-forwarded-for")?.split(",")[0] || "unknown";

  if (rateLimited(`signup-ip-${ip}`, 5, 3600)) {
    return NextResponse.json({ error: "Too many signup attempts." }, { status: 429 });
  }

  const existing = await db.user.findFirst({ where: { email }, select: { id: true } });
  if (existing) return NextResponse.json({ error: "An account with this email already exists." }, { status: 409 });

  const passwordHash = await hashPassword(password);
  const user = await db.user.create({
    data: { name, email, phone: phone || null, username: null, passwordHash, role: "STUDENT", isBanned: false, signupMethod: "credentials" },
  });

  await createSession(user.id);
  await audit({ actorId: user.id, action: "signup", entity: "User", entityId: user.id, ip });

  return NextResponse.json({
    ok: true,
    user: { id: user.id, name: user.name, email: user.email, phone: user.phone, role: user.role, username: user.username },
  });
}
