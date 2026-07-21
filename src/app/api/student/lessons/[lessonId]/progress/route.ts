import { NextResponse, NextRequest } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";

export async function GET(_req: NextRequest, ctx: { params: Promise<{ lessonId: string }> }) {
  const { lessonId } = await ctx.params;
  const user = await getCurrentUser();
  if (!user) return NextResponse.json({ percent: 0 });

  const progress = await db.lessonProgress.findUnique({
    where: { userId_lessonId: { userId: user.id, lessonId } },
  });
  return NextResponse.json({ percent: progress?.percent ?? 0 });
}

export async function POST(req: NextRequest, ctx: { params: Promise<{ lessonId: string }> }) {
  const { lessonId } = await ctx.params;
  const user = await getCurrentUser();
  if (!user) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });

  const body = await req.json().catch(() => ({}));
  const percent = Math.max(0, Math.min(100, Number(body.percent) || 0));
  const completed = Boolean(body.completed) || percent === 100;

  const progress = await db.lessonProgress.upsert({
    where: { userId_lessonId: { userId: user.id, lessonId } },
    create: { userId: user.id, lessonId, percent, completed },
    update: { percent, completed },
  });
  return NextResponse.json(progress);
}
