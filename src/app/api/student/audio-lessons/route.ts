/**
 * GET /api/student/audio-lessons — student view of published lessons
 */
import { NextResponse } from "next/server";
import { db } from "@/lib/db";

export async function GET() {
  const lessons = await db.audioLesson.findMany({
    where: { isPublished: true },
    orderBy: { createdAt: "desc" },
    select: {
      id: true, title: true, slug: true, description: true,
      audioUrl: true, durationSec: true, level: true, category: true, plays: true,
    },
  });
  return NextResponse.json({ lessons });
}
