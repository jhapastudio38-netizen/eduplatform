import { NextResponse, NextRequest } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";
import { createLessonSchema } from "@/lib/security";

export async function GET(_req: NextRequest, ctx: { params: Promise<{ chapterId: string }> }) {
  const { chapterId } = await ctx.params;
  const user = await getCurrentUser();
  if (!user || user.role !== "ADMIN") return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  const lessons = await db.lesson.findMany({
    where: { chapterId },
    orderBy: { order: "asc" },
  });
  return NextResponse.json({ lessons });
}
