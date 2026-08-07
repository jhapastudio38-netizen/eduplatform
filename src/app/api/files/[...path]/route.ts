/**
 * GET /api/files/[...path]
 * Serves files from Cloudflare R2 or local/tmp storage.
 * Students use this URL to view images and listen to audio.
 */
import { NextRequest, NextResponse } from "next/server";
import { getFile } from "@/lib/r2";

export async function GET(_req: NextRequest, ctx: { params: Promise<{ path: string[] }> }) {
  const { path } = await ctx.params;
  // The key is the path joined by "/" — but when stored locally, "/" was replaced with "_"
  // So we try both the original path and the underscore version
  const rawKey = path.join("/");
  
  // Try the original key first (for R2-stored files)
  let file = await getFile(rawKey);
  
  // If not found, try with underscores (for locally-stored files)
  if (!file) {
    const underscoreKey = rawKey.replace(/\//g, "_");
    file = await getFile(underscoreKey);
  }
  
  if (!file) {
    return NextResponse.json({ error: "File not found" }, { status: 404 });
  }
  
  return new NextResponse(file.body, {
    headers: {
      "Content-Type": file.contentType,
      "Cache-Control": "public, max-age=31536000, immutable",
      "Content-Length": file.body.length.toString(),
      "Access-Control-Allow-Origin": "*",
    },
  });
}
