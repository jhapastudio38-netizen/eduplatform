import { NextResponse, NextRequest } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";
import { audit } from "@/lib/audit";

export async function POST(req: NextRequest, ctx: { params: Promise<{ userId: string }> }) {
  const { userId } = await ctx.params;
  const user = await getCurrentUser();
  if (!user || user.role !== "ADMIN") return NextResponse.json({ error: "Forbidden" }, { status: 403 });

  const body = await req.json().catch(() => ({}));
  const ban = Boolean(body.ban);

  // Don't allow banning yourself or other admins (defence in depth)
  if (userId === user.id) return NextResponse.json({ error: "Cannot ban yourself" }, { status: 400 });

  const target = await db.user.findUnique({ where: { id: userId } });
  if (!target) return NextResponse.json({ error: "Not found" }, { status: 404 });
  if (target.role === "ADMIN") return NextResponse.json({ error: "Cannot ban admins" }, { status: 400 });

  await db.user.update({ where: { id: userId }, data: { isBanned: ban } });
  await audit({ actorId: user.id, action: ban ? "ban_user" : "unban_user", entity: "User", entityId: userId });

  // Kill all sessions for the banned user
  if (ban) {
    await db.session.deleteMany({ where: { userId } });
  }
  return NextResponse.json({ ok: true });
}
