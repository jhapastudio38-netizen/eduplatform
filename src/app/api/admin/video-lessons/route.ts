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
  slug: z.string().trim().max(120).optional(), // auto-generated if not provided
  description: z.string().max(2000).optional(),
  youtubeUrl: z.string().optional().or(z.literal("")),
  videoUrl: z.string().optional().or(z.literal("")),
  videoSource: z.enum(["youtube", "upload"]).default("youtube"),
  durationMin: z.number().int().min(1).max(300).default(10),
  level: z.string().max(50).optional(),
  category: z.string().max(50).optional(),
  isPublished: z.boolean().default(true), // default to published
});

// Generate a unique slug from title
async function uniqueSlug(base: string): Promise<string> {
  let slug = base.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "") || "video";
  let n = 1;
  while (await db.videoLesson.findUnique({ where: { slug } })) {
    slug = `${base.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "")}-${++n}`;
    if (slug.length > 120) { slug = `video-${Date.now()}`; break; }
  }
  return slug;
}

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
    youtubeId = "";
    thumbnailUrl = null;
  } else if (d.youtubeUrl) {
    youtubeId = extractYouTubeId(d.youtubeUrl);
    if (!youtubeId) return NextResponse.json({ error: "Invalid YouTube URL — could not extract video ID" }, { status: 400 });
    thumbnailUrl = `https://img.youtube.com/vi/${youtubeId}/maxresdefault.jpg`;
  } else {
    return NextResponse.json({ error: "Either a YouTube URL or an uploaded video is required" }, { status: 400 });
  }

  // Auto-generate unique slug
  const slug = await uniqueSlug(d.slug || d.title);

  try {
    const video = await db.videoLesson.create({
      data: {
        title: d.title,
        slug,
        description: d.description || null,
        youtubeUrl: d.youtubeUrl || "",
        youtubeId,
        videoUrl: d.videoUrl || null,
        videoSource: d.videoSource,
        thumbnailUrl,
        durationMin: d.durationMin,
        level: d.level || null,
        category: d.category || null,
        isPublished: d.isPublished,
      },
    });
    return NextResponse.json({ video });
  } catch (e: any) {
    return NextResponse.json({ error: e.message?.substring(0, 200) || "Failed to create video" }, { status: 500 });
  }
}
