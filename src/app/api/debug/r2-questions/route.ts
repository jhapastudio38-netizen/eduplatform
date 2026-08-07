import { NextResponse } from "next/server";
import { S3Client, ListObjectsV2Command } from "@aws-sdk/client-s3";
export async function GET() {
  const ak = process.env.R2_ACCESS_KEY_ID || "";
  const sk = process.env.R2_SECRET_ACCESS_KEY || "";
  const ep = process.env.R2_ENDPOINT || "";
  const bk = process.env.R2_BUCKET_NAME || "";
  try {
    const c = new S3Client({ region: "auto", endpoint: ep, credentials: { accessKeyId: ak, secretAccessKey: sk } });
    const r: any = await c.send(new ListObjectsV2Command({ Bucket: bk, MaxKeys: 50, Prefix: "questions/" }));
    return NextResponse.json({
      count: r.KeyCount || 0,
      objects: r.Contents?.map((o: any) => ({ key: o.Key, size: o.Size })) || [],
    });
  } catch (e: any) { return NextResponse.json({ error: e.message }, { status: 500 }); }
}
