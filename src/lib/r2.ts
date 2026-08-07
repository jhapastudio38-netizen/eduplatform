/**
 * Cloudflare R2 file storage utility.
 * R2 is S3-compatible, so we use the AWS SDK.
 */
import { S3Client, PutObjectCommand, DeleteObjectCommand, GetObjectCommand } from "@aws-sdk/client-s3";

const R2_ACCOUNT_ID = process.env.R2_ACCOUNT_ID || "";
const R2_ACCESS_KEY_ID = process.env.R2_ACCESS_KEY_ID || "";
const R2_SECRET_ACCESS_KEY = process.env.R2_SECRET_ACCESS_KEY || "";
const R2_ENDPOINT = process.env.R2_ENDPOINT || `https://${R2_ACCOUNT_ID}.r2.cloudflarestorage.com`;
const R2_BUCKET = process.env.R2_BUCKET_NAME || "dreamkorea";

// Create S3 client configured for R2
let _client: S3Client | null = null;

function getClient(): S3Client {
  if (_client) return _client;
  _client = new S3Client({
    region: "auto",
    endpoint: R2_ENDPOINT,
    credentials: {
      accessKeyId: R2_ACCESS_KEY_ID,
      secretAccessKey: R2_SECRET_ACCESS_KEY,
    },
  });
  return _client;
}

/**
 * Upload a file to R2.
 * Returns the public URL (if bucket is public) or the R2 key.
 */
export async function uploadFile(
  key: string,
  body: Buffer | Uint8Array,
  contentType: string
): Promise<{ key: string; url: string }> {
  const client = getClient();
  
  await client.send(
    new PutObjectCommand({
      Bucket: R2_BUCKET,
      Key: key,
      Body: body,
      ContentType: contentType,
    })
  );

  // Return the key — we'll serve files via our API route
  return {
    key,
    url: `/api/files/${key}`,
  };
}

/**
 * Delete a file from R2.
 */
export async function deleteFile(key: string): Promise<void> {
  const client = getClient();
  await client.send(
    new DeleteObjectCommand({
      Bucket: R2_BUCKET,
      Key: key,
    })
  );
}

/**
 * Get a file from R2.
 * Returns the file buffer and content type.
 */
export async function getFile(key: string): Promise<{ body: Buffer; contentType: string } | null> {
  const client = getClient();
  try {
    const response = await client.send(
      new GetObjectCommand({
        Bucket: R2_BUCKET,
        Key: key,
      })
    );
    
    if (!response.Body) return null;
    
    const chunks: Uint8Array[] = [];
    for await (const chunk of response.Body as any) {
      chunks.push(chunk);
    }
    
    return {
      body: Buffer.concat(chunks),
      contentType: response.ContentType || "application/octet-stream",
    };
  } catch {
    return null;
  }
}

/**
 * Generate a unique file key for storage.
 */
export function generateFileKey(folder: string, filename: string): string {
  const ext = filename.split(".").pop() || "";
  const uniqueId = Date.now().toString(36) + Math.random().toString(36).substring(2, 8);
  return `${folder}/${uniqueId}.${ext}`;
}
