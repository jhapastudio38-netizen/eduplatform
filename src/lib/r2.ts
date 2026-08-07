/**
 * Cloudflare R2 file storage utility.
 * R2 is S3-compatible, so we use the AWS SDK.
 * Falls back to local file storage (/public/uploads/) when R2 is not configured.
 */
import { S3Client, PutObjectCommand, DeleteObjectCommand, GetObjectCommand } from "@aws-sdk/client-s3";
import * as fs from "fs";
import * as path from "path";

const R2_ACCOUNT_ID = process.env.R2_ACCOUNT_ID || "";
const R2_ACCESS_KEY_ID = process.env.R2_ACCESS_KEY_ID || "";
const R2_SECRET_ACCESS_KEY = process.env.R2_SECRET_ACCESS_KEY || "";
const R2_ENDPOINT = process.env.R2_ENDPOINT || `https://${R2_ACCOUNT_ID}.r2.cloudflarestorage.com`;
const R2_BUCKET = process.env.R2_BUCKET_NAME || "dreamkorea";

// Check if R2 is configured (has all required credentials)
const R2_CONFIGURED = !!(R2_ACCOUNT_ID && R2_ACCESS_KEY_ID && R2_SECRET_ACCESS_KEY);

// Local storage path (used as fallback when R2 is not configured)
const LOCAL_UPLOAD_DIR = path.join(process.cwd(), "public", "uploads");

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
 * Upload a file to R2 (or local storage as fallback).
 * Returns the public URL path.
 */
export async function uploadFile(
  key: string,
  body: Buffer | Uint8Array,
  contentType: string
): Promise<{ key: string; url: string }> {
  // If R2 is not configured, save to local /public/uploads/ directory
  if (!R2_CONFIGURED) {
    try {
      // Ensure the uploads directory exists
      if (!fs.existsSync(LOCAL_UPLOAD_DIR)) {
        fs.mkdirSync(LOCAL_UPLOAD_DIR, { recursive: true });
      }
      // Save the file locally — use the key as the filename (replace / with _)
      const localKey = key.replace(/\//g, "_");
      const localPath = path.join(LOCAL_UPLOAD_DIR, localKey);
      fs.writeFileSync(localPath, body);
      // Return URL path that Next.js will serve from /public/
      return {
        key: localKey,
        url: `/uploads/${localKey}`,
      };
    } catch (e: any) {
      throw new Error(`Local upload failed: ${e.message}`);
    }
  }

  // R2 upload
  const client = getClient();
  await client.send(
    new PutObjectCommand({
      Bucket: R2_BUCKET,
      Key: key,
      Body: body,
      ContentType: contentType,
    })
  );

  return {
    key,
    url: `/api/files/${key}`,
  };
}

/**
 * Delete a file from R2 (or local storage as fallback).
 */
export async function deleteFile(key: string): Promise<void> {
  if (!R2_CONFIGURED) {
    try {
      const localPath = path.join(LOCAL_UPLOAD_DIR, key);
      if (fs.existsSync(localPath)) fs.unlinkSync(localPath);
    } catch {}
    return;
  }
  const client = getClient();
  await client.send(
    new DeleteObjectCommand({
      Bucket: R2_BUCKET,
      Key: key,
    })
  );
}

/**
 * Get a file from R2 (or local storage as fallback).
 * Returns the file buffer and content type.
 */
export async function getFile(key: string): Promise<{ body: Buffer; contentType: string } | null> {
  // If R2 is not configured, read from local /public/uploads/ directory
  if (!R2_CONFIGURED) {
    try {
      const localPath = path.join(LOCAL_UPLOAD_DIR, key);
      if (!fs.existsSync(localPath)) return null;
      const body = fs.readFileSync(localPath);
      // Guess content type from extension
      const ext = path.extname(key).toLowerCase();
      const contentTypes: Record<string, string> = {
        ".jpg": "image/jpeg", ".jpeg": "image/jpeg", ".png": "image/png",
        ".gif": "image/gif", ".webp": "image/webp", ".svg": "image/svg+xml",
        ".mp3": "audio/mpeg", ".wav": "audio/wav", ".ogg": "audio/ogg",
        ".m4a": "audio/m4a", ".aac": "audio/aac",
        ".mp4": "video/mp4", ".webm": "video/webm",
        ".pdf": "application/pdf",
      };
      return {
        body,
        contentType: contentTypes[ext] || "application/octet-stream",
      };
    } catch {
      return null;
    }
  }

  // R2 download
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
