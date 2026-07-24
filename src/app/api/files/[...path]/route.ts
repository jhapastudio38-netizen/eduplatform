/**
 * GET /api/files/[...path]
 * Serves files from Cloudflare R2.
 * Students use this URL to view PDFs, images, and listen to audio.
 */
import { NextRequest, NextResponse } from "next/server";
import { getFile } from "@/lib/r2";

export async function GET(_req: NextRequest, ctx: { params: Promise<{ path: string[] }> }) {
  const { path } = await ctx.params;
  const key = path.join("/");
  
  const file = await getFile(key);
  if (!file) {
    return NextResponse.json({ error: "File not found" }, { status: 404 });
  }
  
  return new NextResponse(file.body, {
    headers: {
      "Content-Type": file.contentType,
      "Cache-Control": "public, max-age=31536000, immutable",
      "Content-Length": file.body.length.toString(),
    },
  });
}
