/**
 * POST /api/admin/upload
 *
 * Universal file upload endpoint for admin.
 * Accepts: multipart/form-data with fields:
 *   - file: the file to upload (image, audio, PDF, video)
 *   - folder: optional subfolder name (e.g. "covers", "books", "questions", "audio", "videos")
 *
 * Returns: { ok: true, url: string, key: string, size: number, contentType: string }
 *
 * Files are stored in Cloudflare R2 and served via /api/files/[...path].
 */
import { NextRequest, NextResponse } from "next/server";
import { getCurrentUser } from "@/lib/session";
import { uploadFile, generateFileKey } from "@/lib/r2";

const MAX_SIZE = 100 * 1024 * 1024; // 100MB

const ALLOWED_TYPES: Record<string, string[]> = {
  image: ["image/jpeg", "image/png", "image/webp", "image/gif", "image/svg+xml"],
  audio: ["audio/mpeg", "audio/mp3", "audio/wav", "audio/ogg", "audio/m4a", "audio/aac", "audio/x-m4a"],
  video: ["video/mp4", "video/webm", "video/ogg", "video/quicktime", "video/x-matroska"],
  document: ["application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"],
};

function getAllowedExtensions(): string[] {
  return [".jpg", ".jpeg", ".png", ".webp", ".gif", ".svg", ".mp3", ".wav", ".ogg", ".m4a", ".aac", ".mp4", ".webm", ".mov", ".mkv", ".pdf", ".doc", ".docx"];
}

export async function POST(req: NextRequest) {
  const user = await getCurrentUser(req);
  if (!user || (user.role !== "ADMIN" && user.role !== "TEACHER")) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }

  try {
    const formData = await req.formData();
    const file = formData.get("file");
    const folder = (formData.get("folder") as string) || "uploads";

    if (!file || !(file instanceof File)) {
      return NextResponse.json({ error: "No file provided" }, { status: 400 });
    }

    // Validate size
    if (file.size > MAX_SIZE) {
      return NextResponse.json(
        { error: `File too large. Maximum is 100MB. Your file is ${(file.size / 1024 / 1024).toFixed(1)}MB.` },
        { status: 413 },
      );
    }

    // Read file buffer
    const arrayBuffer = await file.arrayBuffer();
    const buffer = Buffer.from(arrayBuffer);

    // Generate unique key with folder prefix
    const key = generateFileKey(folder, file.name || "upload");
    const contentType = file.type || "application/octet-stream";

    const { url } = await uploadFile(key, buffer, contentType);

    return NextResponse.json({
      ok: true,
      url,
      key,
      size: file.size,
      contentType,
      originalName: file.name,
    });
  } catch (e: any) {
    console.error("Upload failed:", e);
    return NextResponse.json(
      { error: `Upload failed: ${e.message?.substring(0, 150)}` },
      { status: 500 },
    );
  }
}
// trigger rebuild Sat Jul 25 12:37:01 UTC 2026

