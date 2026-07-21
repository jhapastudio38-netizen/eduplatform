import { NextResponse, NextRequest } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";

export async function POST(_req: NextRequest, ctx: { params: Promise<{ classId: string }> }) {
  const user = await getCurrentUser();
  if (!user || user.role !== "TEACHER") {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  const { classId } = await ctx.params;
  await db.liveClass.updateMany({
    where: { id: classId, teacherId: user.id },
    data: { isLive: false, endedAt: new Date() },
  });
  return NextResponse.json({ ok: true });
}

export async function DELETE(_req: NextRequest, ctx: { params: Promise<{ classId: string }> }) {
  const user = await getCurrentUser();
  if (!user || user.role !== "TEACHER") {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  const { classId } = await ctx.params;
  await db.liveClass.deleteMany({ where: { id: classId, teacherId: user.id } });
  return NextResponse.json({ ok: true });
}
