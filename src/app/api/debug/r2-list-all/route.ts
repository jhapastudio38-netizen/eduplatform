import { NextResponse } from "next/server";
import { S3Client, ListObjectsV2Command } from "@aws-sdk/client-s3";
export async function GET() {
  const ak = process.env.R2_ACCESS_KEY_ID || "";
  const sk = process.env.R2_SECRET_ACCESS_KEY || "";
  const ep = process.env.R2_ENDPOINT || "";
  const bk = process.env.R2_BUCKET_NAME || "";
  try {
    const c = new S3Client({ region: "auto", endpoint: ep, credentials: { accessKeyId: ak, secretAccessKey: sk } });
    const all: any[] = [];
    let tok: string | undefined;
    do {
      const r: any = await c.send(new ListObjectsV2Command({ Bucket: bk, MaxKeys: 1000, ContinuationToken: tok }));
      if (r.Contents) all.push(...r.Contents.map((o: any) => ({ key: o.Key, size: o.Size })));
      tok = r.IsTruncated ? r.NextContinuationToken : undefined;
    } while (tok);
    const folders: Record<string, number> = {};
    for (const o of all) { const f = o.key.split("/")[0] || "root"; folders[f] = (folders[f]||0)+1; }
    return NextResponse.json({ total: all.length, folders, sample: all.slice(0,30) });
  } catch (e: any) { return NextResponse.json({ error: e.message }, { status: 500 }); }
}
