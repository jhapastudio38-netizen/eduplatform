/**
 * Session management.
 *
 * Strategy: stateless session token stored in an httpOnly cookie.
 * Token = random 32 bytes (hex). Server stores sha256(token) in DB
 * so a DB leak does not compromise active sessions.
 *
 * For 100k concurrency on AWS, the Session table is the hot path.
 * Options to scale:
 *   - Move sessions to Redis (read-through cache in front of DB).
 *   - Switch to JWT signed with a rotating key (stateless, no DB read).
 * The current API (`getSession`, `createSession`) hides the storage detail
 * so the migration is local to this file.
 */

import { cookies } from "next/headers";
import { db } from "@/lib/db";
import { CONFIG } from "@/lib/config";
import { generateToken, sha256 } from "@/lib/security";

export const SESSION_COOKIE = "ep_sid";
const SESSION_TTL_MS = CONFIG.auth.sessionTtlDays * 24 * 60 * 60 * 1000;

export async function createSession(userId: string): Promise<string> {
  const token = generateToken(32);
  const expiresAt = new Date(Date.now() + SESSION_TTL_MS);

  // Store only the hash. The cookie carries the raw token.
  await db.session.create({
    data: {
      userId,
      token: sha256(token),
      expiresAt,
    },
  });

  const cookieStore = await cookies();
  cookieStore.set(SESSION_COOKIE, token, {
    httpOnly: true,
    // Only require HTTPS when we actually have a cert — the ALB is HTTP-only
    // until ACM validates. Once HTTPS is live, this auto-enables Secure.
    secure: true, // HTTPS is now live via ACM cert
    sameSite: "lax",
    path: "/",
    expires: expiresAt,
  });

  return token;
}

export async function destroySession(): Promise<void> {
  const cookieStore = await cookies();
  const token = cookieStore.get(SESSION_COOKIE)?.value;
  if (token) {
    await db.session
      .deleteMany({ where: { token: sha256(token) } })
      .catch(() => null);
  }
  cookieStore.delete(SESSION_COOKIE);
}

export async function getSession() {
  const cookieStore = await cookies();
  const token = cookieStore.get(SESSION_COOKIE)?.value;
  if (!token) return null;

  const session = await db.session.findUnique({
    where: { token: sha256(token) },
    include: { user: true },
  });

  if (!session) return null;
  if (session.expiresAt < new Date()) {
    await db.session.delete({ where: { id: session.id } }).catch(() => null);
    return null;
  }
  if (session.user.isBanned) return null;

  return session;
}

export async function getCurrentUser() {
  const s = await getSession();
  return s?.user ?? null;
}

export async function requireRole(role: "STUDENT" | "TEACHER" | "ADMIN") {
  const user = await getCurrentUser();
  if (!user) {
    throw new Response("Unauthorized", { status: 401 });
  }
  if (user.role !== role) {
    throw new Response("Forbidden", { status: 403 });
  }
  return user;
}
