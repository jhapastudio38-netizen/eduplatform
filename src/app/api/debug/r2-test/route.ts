import { NextResponse } from "next/server";
import { S3Client, ListBucketsCommand } from "@aws-sdk/client-s3";

export async function GET() {
  const R2_ACCOUNT_ID = process.env.R2_ACCOUNT_ID || "";
  const R2_ACCESS_KEY_ID = process.env.R2_ACCESS_KEY_ID || "";
  const R2_SECRET_ACCESS_KEY = process.env.R2_SECRET_ACCESS_KEY || "";
  const R2_BUCKET = process.env.R2_BUCKET_NAME || "";
  const R2_ENDPOINT = process.env.R2_ENDPOINT || "";

  const configured = !!(R2_ACCOUNT_ID && R2_ACCESS_KEY_ID && R2_SECRET_ACCESS_KEY);
  
  if (!configured) {
    return NextResponse.json({
      configured: false,
      R2_ACCOUNT_ID: R2_ACCOUNT_ID ? "SET" : "EMPTY",
      R2_ACCESS_KEY_ID: R2_ACCESS_KEY_ID ? "SET" : "EMPTY",
      R2_SECRET_ACCESS_KEY: R2_SECRET_ACCESS_KEY ? "SET" : "EMPTY",
      R2_BUCKET_NAME: R2_BUCKET || "EMPTY",
      R2_ENDPOINT: R2_ENDPOINT ? "SET" : "EMPTY",
    });
  }

  try {
    const client = new S3Client({
      region: "auto",
      endpoint: R2_ENDPOINT,
      credentials: {
        accessKeyId: R2_ACCESS_KEY_ID,
        secretAccessKey: R2_SECRET_ACCESS_KEY,
      },
    });
    
    const response = await client.send(new ListBucketsCommand({}));
    return NextResponse.json({
      configured: true,
      buckets: response.Buckets?.map(b => b.Name) || [],
    });
  } catch (e: any) {
    return NextResponse.json({
      configured: true,
      error: e.message,
    });
  }
}
