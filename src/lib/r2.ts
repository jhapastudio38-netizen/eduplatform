/**
 * Cloudflare R2 file storage utility.
 * R2 is S3-compatible, so we use the AWS SDK.
 * Falls back to /tmp/ storage when R2 is not configured (Vercel serverless).
 * Files in /tmp/ are served via /api/files/[...path] route.
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

// Use /tmp/ on Vercel (only writable directory in serverless), or /public/uploads/ locally
const IS_VERCEL = !!process.env.VERCEL;
const LOCAL_UPLOAD_DIR = IS_VERCEL
  ? path.join("/tmp", "uploads")
  : path.join(process.cwd(), "public", "uploads");

// In-memory cache for uploaded files (Vercel serverless /tmp/ is ephemeral)
// This maps key -> { body, contentType } so files survive within a function instance
const fileCache = new Map<string, { body: Buffer; contentType: string }>();

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
 * Ensure the upload directory exists
 */
function ensureUploadDir() {
  try {
    if (!fs.existsSync(LOCAL_UPLOAD_DIR)) {
      fs.mkdirSync(LOCAL_UPLOAD_DIR, { recursive: true });
    }
  } catch (e) {
    // Ignore — might fail on read-only FS, we'll use in-memory cache
  }
}

/**
 * Upload a file to R2 (or local/tmp storage as fallback).
 * Returns the public URL path.
 */
export async function uploadFile(
  key: string,
  body: Buffer | Uint8Array,
  contentType: string
): Promise<{ key: string; url: string }> {
  const buf = Buffer.isBuffer(body) ? body : Buffer.from(body);

  // If R2 is not configured, save to /tmp/ (Vercel) or /public/uploads/ (local)
  if (!R2_CONFIGURED) {
    try {
      ensureUploadDir();
      const localKey = key.replace(/\//g, "_");
      const localPath = path.join(LOCAL_UPLOAD_DIR, localKey);

      // Write to filesystem
      try {
        fs.writeFileSync(localPath, buf);
      } catch (e) {
        // If FS write fails (read-only), use in-memory cache only
      }

      // Always cache in memory (survives within the same serverless instance)
      fileCache.set(localKey, { body: buf, contentType });

      // Return URL — served via /api/files/ route which checks cache + FS
      return {
        key: localKey,
        url: `/api/files/${localKey}`,
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
      Body: buf,
      ContentType: contentType,
    })
  );

  return {
    key,
    url: `/api/files/${key}`,
  };
}

/**
 * Delete a file from R2 (or local/tmp storage as fallback).
 */
export async function deleteFile(key: string): Promise<void> {
  if (!R2_CONFIGURED) {
    try {
      const localPath = path.join(LOCAL_UPLOAD_DIR, key);
      if (fs.existsSync(localPath)) fs.unlinkSync(localPath);
    } catch {}
    fileCache.delete(key);
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
 * Get a file from R2 (or local/tmp/in-memory cache as fallback).
 * Returns the file buffer and content type.
 */
export async function getFile(key: string): Promise<{ body: Buffer; contentType: string } | null> {
  // If R2 is not configured, read from cache/FS
  if (!R2_CONFIGURED) {
    // Check in-memory cache first
    const cached = fileCache.get(key);
    if (cached) {
      return cached;
    }

    // Try filesystem
    try {
      const localPath = path.join(LOCAL_UPLOAD_DIR, key);
      if (fs.existsSync(localPath)) {
        const body = fs.readFileSync(localPath);
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
      }
    } catch {}

    return null;
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
