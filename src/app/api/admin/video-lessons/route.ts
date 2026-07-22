import { NextResponse, NextRequest } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";
import { z } from "zod";

export async function GET() {
  const user = await getCurrentUser();
  if (!user || (user.role !== "ADMIN" && user.role !== "TEACHER")) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  const videos = await db.videoLesson.findMany({ orderBy: { sortOrder: "asc" } });
  return NextResponse.json({ videos });
}

function extractYouTubeId(url: string): string {
  const patterns = [
    /(?:youtube\.com\/watch\?v=|youtu\.be\/|youtube\.com\/embed\/|youtube\.com\/shorts\/)([a-zA-Z0-9_-]{11})/,
  ];
  for (const p of patterns) {
    const m = url.match(p);
    if (m) return m[1];
  }
  if (url.length === 11) return url;
  return "";
}

const schema = z.object({
  title: z.string().trim().min(2).max(200),
  slug: z.string().trim().min(2).max(120).regex(/^[a-z0-9-]+$/),
  description: z.string().max(2000).optional(),
  youtubeUrl: z.string().url(),
  durationMin: z.number().int().min(1).max(300).default(10),
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
  if (!parsed.success) return NextResponse.json({ error: parsed.error.issues[0]?.message }, { status: 400 });

  const youtubeId = extractYouTubeId(parsed.data.youtubeUrl);
  if (!youtubeId) return NextResponse.json({ error: "Invalid YouTube URL" }, { status: 400 });

  const thumbnailUrl = `https://img.youtube.com/vi/${youtubeId}/maxresdefault.jpg`;

  try {
    const video = await db.videoLesson.create({
      data: { ...parsed.data, youtubeId, thumbnailUrl },
    });
    return NextResponse.json({ video });
  } catch {
    return NextResponse.json({ error: "Slug already exists" }, { status: 409 });
  }
}
