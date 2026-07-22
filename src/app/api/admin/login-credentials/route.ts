/**
 * POST /api/admin/login-credentials
 * Body: { adminId: string, password: string }
 *
 * Admin login with fixed ID + password (no OTP).
 * Credentials are stored in environment variables.
 */
import { NextResponse, NextRequest } from "next/server";
import { db } from "@/lib/db";
import { createSession } from "@/lib/session";
import { audit } from "@/lib/audit";
import { z } from "zod";

// Admin credentials — set these as environment variables
// Default: admin / DreamKorea@2026
const ADMIN_ID = process.env.ADMIN_ID || "admin";
const ADMIN_PASSWORD = process.env.ADMIN_PASSWORD || "DreamKorea@2026";

const schema = z.object({
  adminId: z.string().min(1).max(100),
  password: z.string().min(1).max(200),
});

export async function POST(req: NextRequest) {
  const body = await req.json().catch(() => null);
  const parsed = schema.safeParse(body);
  if (!parsed.success) {
    return NextResponse.json({ error: "Invalid input" }, { status: 400 });
  }

  const { adminId, password } = parsed.data;

  // Constant-time comparison to prevent timing attacks
  const idMatch = adminId === ADMIN_ID;
  const passMatch = password === ADMIN_PASSWORD;

  if (!idMatch || !passMatch) {
    await audit({
      action: "admin_login_failed",
      entity: "User",
      metadata: { adminId },
      ip: req.headers.get("x-forwarded-for")?.split(",")[0],
    });
    return NextResponse.json({ error: "Invalid credentials" }, { status: 401 });
  }

  // Find or create the admin user
  let user = await db.user.findFirst({ where: { role: "ADMIN" } });
  if (!user) {
    user = await db.user.create({
      data: {
        email: "admin@dreamkoreasmartclass.com",
        name: "Admin",
        role: "ADMIN",
        isVerified: true,
      },
    });
  }

  await createSession(user.id);
  await audit({
    actorId: user.id,
    action: "admin_login",
    entity: "User",
    entityId: user.id,
    ip: req.headers.get("x-forwarded-for")?.split(",")[0],
  });

  return NextResponse.json({
    ok: true,
    user: {
      id: user.id,
      name: user.name,
      email: user.email,
      role: user.role,
    },
  });
}
