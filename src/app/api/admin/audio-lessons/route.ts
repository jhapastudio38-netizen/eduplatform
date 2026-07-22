/**
 * GET /api/student/audio-lessons — list published audio lessons
 * POST /api/admin/audio-lessons — admin creates a lesson
 */
import { NextResponse, NextRequest } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";
import { z } from "zod";

export async function GET() {
  const lessons = await db.audioLesson.findMany({
    where: { isPublished: true },
    orderBy: { createdAt: "desc" },
  });
  return NextResponse.json({ lessons });
}

const schema = z.object({
  title: z.string().trim().min(2).max(200),
  slug: z.string().trim().min(2).max(120).regex(/^[a-z0-9-]+$/),
  description: z.string().max(2000).optional(),
  audioUrl: z.string().url(),
  durationSec: z.number().int().min(1).max(7200),
  transcript: z.string().max(50000).optional(),
  translation: z.string().max(50000).optional(),
  level: z.string().max(50).optional(),
  category: z.string().max(50).optional(),
  isPublished: z.boolean().default(false),
});

export async function POST(req: NextRequest) {
  const user = await getCurrentUser();
  if (!user || (user.role !== "ADMIN" && user.role !== "TEACHER")) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  const body = await req.json().catch(() => null);
  const parsed = schema.safeParse(body);
  if (!parsed.success) {
    return NextResponse.json({ error: parsed.error.issues[0]?.message }, { status: 400 });
  }
  try {
    const lesson = await db.audioLesson.create({ data: parsed.data });
    return NextResponse.json({ lesson });
  } catch {
    return NextResponse.json({ error: "Slug already exists" }, { status: 409 });
  }
}
