import { NextResponse } from "next/server";
import { S3Client, ListObjectsV2Command } from "@aws-sdk/client-s3";

export async function GET() {
  const R2_ACCESS_KEY_ID = process.env.R2_ACCESS_KEY_ID || "";
  const R2_SECRET_ACCESS_KEY = process.env.R2_SECRET_ACCESS_KEY || "";
  const R2_ENDPOINT = process.env.R2_ENDPOINT || "";
  const R2_BUCKET = process.env.R2_BUCKET_NAME || "";

  try {
    const client = new S3Client({
      region: "auto",
      endpoint: R2_ENDPOINT,
      credentials: { accessKeyId: R2_ACCESS_KEY_ID, secretAccessKey: R2_SECRET_ACCESS_KEY },
    });
    
    const response = await client.send(new ListObjectsV2Command({
      Bucket: R2_BUCKET,
      MaxKeys: 20,
    }));
    
    return NextResponse.json({
      bucket: R2_BUCKET,
      objects: response.Contents?.map(o => ({ key: o.Key, size: o.Size })) || [],
      count: response.KeyCount || 0,
    });
  } catch (e: any) {
    return NextResponse.json({ error: e.message }, { status: 500 });
  }
}
