/**
 * POST /api/admin/upload-video
 *
 * Uploads a video file from the admin's device to Cloudflare R2.
 * Returns the URL that can be used in the VideoLesson.videoUrl field.
 *
 * Accepts: multipart/form-data with a "file" field
 * Returns: { url: string, key: string, size: number }
 */
import { NextRequest, NextResponse } from "next/server";
import { getCurrentUser } from "@/lib/session";
import { uploadFile, generateFileKey } from "@/lib/r2";

const MAX_VIDEO_SIZE = 100 * 1024 * 1024; // 100MB
const ALLOWED_TYPES = [
  "video/mp4",
  "video/webm",
  "video/ogg",
  "video/quicktime",
  "video/x-matroska",
];

export async function POST(req: NextRequest) {
  const user = await getCurrentUser(req);
  if (!user || (user.role !== "ADMIN" && user.role !== "TEACHER")) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }

  try {
    const formData = await req.formData();
    const file = formData.get("file");
    if (!file || !(file instanceof File)) {
      return NextResponse.json({ error: "No file provided" }, { status: 400 });
    }

    // Validate type
    if (!ALLOWED_TYPES.includes(file.type)) {
      return NextResponse.json(
        { error: `Unsupported video type: ${file.type}. Use MP4, WebM, MOV, or OGG.` },
        { status: 400 },
      );
    }

    // Validate size
    if (file.size > MAX_VIDEO_SIZE) {
      return NextResponse.json(
        { error: `Video too large. Maximum is 100MB. Your file is ${(file.size / 1024 / 1024).toFixed(1)}MB.` },
        { status: 413 },
      );
    }

    // Read file buffer
    const arrayBuffer = await file.arrayBuffer();
    const buffer = Buffer.from(arrayBuffer);

    // Generate unique key and upload
    const key = generateFileKey("videos", file.name || "upload.mp4");
    const { url } = await uploadFile(key, buffer, file.type);

    return NextResponse.json({
      url,
      key,
      size: file.size,
      contentType: file.type,
    });
  } catch (e: any) {
    console.error("Video upload failed:", e);
    return NextResponse.json(
      { error: `Upload failed: ${e.message?.substring(0, 150)}` },
      { status: 500 },
    );
  }
}
