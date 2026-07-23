/**
 * POST /api/admin/login-credentials
 * Body: { adminId: string, password: string }
 *
 * Admin login with fixed ID + password (no OTP, no database needed).
 * Sets a simple session cookie that works even if the database is down.
 */
import { NextResponse, NextRequest } from "next/server";
import { z } from "zod";
import { generateToken } from "@/lib/security";

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

  if (adminId !== ADMIN_ID || password !== ADMIN_PASSWORD) {
    return NextResponse.json({ error: "Invalid admin ID or password" }, { status: 401 });
  }

  // Create a simple session token (no database needed)
  const token = generateToken(32);
  const expiresAt = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000); // 7 days

  const res = NextResponse.json({
    ok: true,
    user: {
      id: "admin",
      name: "Admin",
      email: "admin@dreamkoreasmartclass.com",
      role: "ADMIN",
    },
  });

  // Set session cookie (httpOnly, secure, 7 days)
  res.cookies.set("ep_sid", token, {
    httpOnly: true,
    secure: true,
    sameSite: "lax",
    expires: expiresAt,
    path: "/",
  });

  // Also set an admin flag cookie (so the frontend knows this is admin)
  res.cookies.set("ep_role", "ADMIN", {
    httpOnly: false,
    secure: true,
    sameSite: "lax",
    expires: expiresAt,
    path: "/",
  });

  return res;
}
