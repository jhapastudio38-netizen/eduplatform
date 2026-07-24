import { NextResponse, NextRequest } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";
import { z } from "zod";

export async function GET(req: NextRequest) {
  const user = await getCurrentUser(req);
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
  youtubeUrl: z.string().url().optional().or(z.literal("")),
  videoUrl: z.string().optional().or(z.literal("")),
  videoSource: z.enum(["youtube", "upload"]).default("youtube"),
  durationMin: z.number().int().min(1).max(300).default(10),
  level: z.string().max(50).optional(),
  category: z.string().max(50).optional(),
  isPublished: z.boolean().default(false),
});

export async function POST(req: NextRequest) {
  const user = await getCurrentUser(req);
  if (!user || (user.role !== "ADMIN" && user.role !== "TEACHER")) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  const body = await req.json().catch(() => null);
  const parsed = schema.safeParse(body);
  if (!parsed.success) return NextResponse.json({ error: parsed.error.issues[0]?.message }, { status: 400 });

  const d = parsed.data;
  let youtubeId = "";
  let thumbnailUrl: string | null = null;

  if (d.videoSource === "upload" && d.videoUrl) {
    // Uploaded video — no YouTube ID needed
    youtubeId = "";
    thumbnailUrl = null;
  } else if (d.youtubeUrl) {
    youtubeId = extractYouTubeId(d.youtubeUrl);
    if (!youtubeId) return NextResponse.json({ error: "Invalid YouTube URL" }, { status: 400 });
    thumbnailUrl = `https://img.youtube.com/vi/${youtubeId}/maxresdefault.jpg`;
  } else {
    return NextResponse.json({ error: "Either a YouTube URL or an uploaded video is required" }, { status: 400 });
  }

  try {
    const video = await db.videoLesson.create({
      data: {
        title: d.title,
        slug: d.slug,
        description: d.description,
        youtubeUrl: d.youtubeUrl || "",
        youtubeId,
        videoUrl: d.videoUrl || null,
        videoSource: d.videoSource,
        thumbnailUrl,
        durationMin: d.durationMin,
        level: d.level,
        category: d.category,
        isPublished: d.isPublished,
      },
    });
    return NextResponse.json({ video });
  } catch {
    return NextResponse.json({ error: "Slug already exists" }, { status: 409 });
  }
}
