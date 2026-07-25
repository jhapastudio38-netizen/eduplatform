import { NextRequest, NextResponse } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";

export async function DELETE(req: NextRequest, ctx: { params: Promise<{ sessionId: string }> }) {
  const user = await getCurrentUser(req);
  if (!user || (user.role !== "ADMIN" && user.role !== "TEACHER")) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  const { sessionId } = await ctx.params;
  try {
    await db.liveSession.delete({ where: { id: sessionId } });
    return NextResponse.json({ ok: true });
  } catch {
    return NextResponse.json({ error: "Not found" }, { status: 404 });
  }
}

export async function PATCH(req: NextRequest, ctx: { params: Promise<{ sessionId: string }> }) {
  const user = await getCurrentUser(req);
  if (!user || (user.role !== "ADMIN" && user.role !== "TEACHER")) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  const { sessionId } = await ctx.params;
  const body = await req.json().catch(() => ({}));
  try {
    const updated = await db.liveSession.update({
      where: { id: sessionId },
      data: { isActive: body.isActive },
    });
    return NextResponse.json({ session: updated });
  } catch {
    return NextResponse.json({ error: "Not found" }, { status: 404 });
  }
}
