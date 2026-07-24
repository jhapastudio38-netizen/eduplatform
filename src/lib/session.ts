import { cookies } from "next/headers";
import { db } from "@/lib/db";
import { CONFIG } from "@/lib/config";
import { generateToken, sha256 } from "@/lib/security";
import type { NextRequest } from "next/server";

export const SESSION_COOKIE = "ep_sid";
const SESSION_TTL_MS = CONFIG.auth.sessionTtlDays * 24 * 60 * 60 * 1000;

export async function createSession(userId: string): Promise<string> {
  const token = generateToken(32);
  const expiresAt = new Date(Date.now() + SESSION_TTL_MS);
  try {
    await db.session.create({ data: { userId, token: sha256(token), expiresAt } });
  } catch (e) { console.error("Session save failed:", e); }
  const cookieStore = await cookies();
  // Note: secure=true requires HTTPS (always true on Vercel).
  // sameSite=lax allows the cookie to be sent on same-site API requests.
  cookieStore.set(SESSION_COOKIE, token, {
    httpOnly: true,
    secure: true,
    sameSite: "lax",
    expires: expiresAt,
    path: "/",
  });
  return token;
}

export async function getCurrentUser(req?: NextRequest) {
  let sessionToken: string | undefined;
  let roleCookie: string | undefined;

  // Try reading from request first (more reliable in serverless)
  if (req) {
    sessionToken = req.cookies.get(SESSION_COOKIE)?.value;
    roleCookie = req.cookies.get("ep_role")?.value;
  }

  // Fallback to next/headers cookies()
  if (!sessionToken) {
    try {
      const cookieStore = await cookies();
      sessionToken = cookieStore.get(SESSION_COOKIE)?.value;
      roleCookie = cookieStore.get("ep_role")?.value;
    } catch {}
  }

  if (!sessionToken) return null;

  // Admin shortcut - no DB needed
  if (roleCookie === "ADMIN") {
    return {
      id: "admin",
      name: "Admin",
      email: "admin@dreamkoreasmartclass.com",
      role: "ADMIN" as const,
      phone: null,
      avatarUrl: null,
      isBanned: false,
      isVerified: true,
    };
  }

  // DB lookup for students/teachers
  try {
    const hashedToken = sha256(sessionToken);
    const session = await db.session.findFirst({
      where: { token: hashedToken, expiresAt: { gt: new Date() } },
    });
    if (!session) return null;
    const user = await db.user.findUnique({ where: { id: session.userId } });
    if (!user || user.isBanned) return null;
    return {
      id: user.id, name: user.name, email: user.email, role: user.role,
      phone: user.phone, avatarUrl: user.avatarUrl, isBanned: user.isBanned, isVerified: user.isVerified,
    };
  } catch (e) {
    console.error("Session lookup error:", e);
    return null;
  }
}

export async function clearSession() {
  const cookieStore = await cookies();
  cookieStore.delete(SESSION_COOKIE);
  cookieStore.delete("ep_role");
}
