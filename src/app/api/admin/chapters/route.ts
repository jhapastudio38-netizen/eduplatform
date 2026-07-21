import { NextResponse, NextRequest } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";
import { createChapterSchema } from "@/lib/security";
import { audit } from "@/lib/audit";

export async function GET() {
  const user = await getCurrentUser();
  if (!user || user.role !== "ADMIN") return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  const chapters = await db.chapter.findMany({
    orderBy: [{ subjectId: "asc" }, { order: "asc" }],
    include: { _count: { select: { lessons: true, questions: true } }, subject: { select: { name: true } } },
  });
  return NextResponse.json({ chapters });
}

export async function POST(req: NextRequest) {
  const user = await getCurrentUser();
  if (!user || user.role !== "ADMIN") return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  const body = await req.json().catch(() => null);
  const parsed = createChapterSchema.safeParse(body);
  if (!parsed.success) return NextResponse.json({ error: parsed.error.issues[0]?.message }, { status: 400 });
  try {
    const c = await db.chapter.create({
      data: { ...parsed.data, authorId: user.id, isPublished: true },
    });
    await audit({ actorId: user.id, action: "create_chapter", entity: "Chapter", entityId: c.id, metadata: { title: c.title } });
    return NextResponse.json({ chapter: c });
  } catch {
    return NextResponse.json({ error: "Slug already exists for this subject" }, { status: 409 });
  }
}
