import { NextResponse, NextRequest } from "next/server";
import { db } from "@/lib/db";

export async function GET(_req: NextRequest, ctx: { params: Promise<{ subjectId: string }> }) {
  const { subjectId } = await ctx.params;
  const chapters = await db.chapter.findMany({
    where: { subjectId, isPublished: true },
    orderBy: { order: "asc" },
    select: {
      id: true, subjectId: true, title: true, slug: true,
      description: true, order: true, isPublished: true,
    },
  });
  return NextResponse.json({ chapters });
}
