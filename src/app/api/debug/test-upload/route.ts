import { NextRequest, NextResponse } from "next/server";
import { uploadFile, generateFileKey } from "@/lib/r2";

export async function POST(req: NextRequest) {
  try {
    const formData = await req.formData();
    const file = formData.get("file");
    if (!file || !(file instanceof File)) {
      return NextResponse.json({ error: "No file" }, { status: 400 });
    }
    
    const buffer = Buffer.from(await file.arrayBuffer());
    const key = generateFileKey("questions", file.name || "test.jpg");
    const contentType = file.type || "image/jpeg";
    
    const result = await uploadFile(key, buffer, contentType);
    
    return NextResponse.json({
      ok: true,
      key: result.key,
      url: result.url,
      size: file.size,
      contentType,
    });
  } catch (e: any) {
    return NextResponse.json({ error: e.message }, { status: 500 });
  }
}
