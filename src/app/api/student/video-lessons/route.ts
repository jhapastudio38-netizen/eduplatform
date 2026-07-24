import { NextResponse } from "next/server";
import { db } from "@/lib/db";

export async function GET() {
  const videos = await db.videoLesson.findMany({
    where: { isPublished: true },
    orderBy: { sortOrder: "asc" },
    select: {
      id: true, title: true, slug: true, description: true,
      youtubeId: true, videoUrl: true, videoSource: true,
      thumbnailUrl: true, durationMin: true,
      level: true, category: true, views: true,
    },
  });
  return NextResponse.json({ videos });
}
