import { NextResponse, NextRequest } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";
import { createLessonSchema } from "@/lib/security";
import { audit } from "@/lib/audit";

export async function GET() {
  const user = await getCurrentUser();
  if (!user || user.role !== "ADMIN") return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  const lessons = await db.lesson.findMany({
    orderBy: { createdAt: "desc" },
    take: 100,
    include: { chapter: { select: { title: true } } },
  });
  return NextResponse.json({ lessons });
}

export async function POST(req: NextRequest) {
  const user = await getCurrentUser();
  if (!user || user.role !== "ADMIN") return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  const body = await req.json().catch(() => null);
  const parsed = createLessonSchema.safeParse(body);
  if (!parsed.success) return NextResponse.json({ error: parsed.error.issues[0]?.message }, { status: 400 });

  const { chapterId, title, slug, type, content, videoUrl, durationMin, order } = parsed.data;
  try {
    const lesson = await db.lesson.create({
      data: {
        chapterId, title, slug, type, content,
        videoUrl: videoUrl || null,
        durationMin,
        order: order || 0,
        isPublished: true,
      },
    });
    await audit({ actorId: user.id, action: "create_lesson", entity: "Lesson", entityId: lesson.id, metadata: { title } });
    return NextResponse.json({ lesson });
  } catch {
    return NextResponse.json({ error: "Slug already exists in this chapter" }, { status: 409 });
  }
}
