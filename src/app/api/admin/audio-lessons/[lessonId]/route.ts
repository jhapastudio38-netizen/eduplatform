import { NextResponse, NextRequest } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";

export async function DELETE(_req: NextRequest, ctx: { params: Promise<{ lessonId: string }> }) {
  const { lessonId } = await ctx.params;
  const user = await getCurrentUser();
  if (!user || (user.role !== "ADMIN" && user.role !== "TEACHER")) return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  await db.audioLesson.delete({ where: { id: lessonId } }).catch(() => {});
  return NextResponse.json({ ok: true });
}
