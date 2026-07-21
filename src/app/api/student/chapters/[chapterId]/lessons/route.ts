import { NextResponse, NextRequest } from "next/server";
import { db } from "@/lib/db";

export async function GET(_req: NextRequest, ctx: { params: Promise<{ chapterId: string }> }) {
  const { chapterId } = await ctx.params;
  const lessons = await db.lesson.findMany({
    where: { chapterId, isPublished: true },
    orderBy: { order: "asc" },
    select: {
      id: true, chapterId: true, title: true, slug: true, type: true,
      durationMin: true, order: true, isPublished: true,
      videoUrl: true,
    },
  });
  return NextResponse.json({ lessons });
}
