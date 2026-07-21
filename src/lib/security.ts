/**
 * Security utilities — input validation, sanitization, hashing.
 * All user input MUST pass through Zod schemas before hitting the DB.
 * Prisma is used with parameterized queries only (no raw SQL → no SQL injection).
 */

import { createHash, randomBytes, timingSafeEqual } from "crypto";
import { z } from "zod";

// ─── Hashing (for OTP storage & session tokens) ───────────────────────────────

export function sha256(input: string): string {
  return createHash("sha256").update(input, "utf8").digest("hex");
}

export function generateToken(bytes = 32): string {
  return randomBytes(bytes).toString("hex");
}

export function constantTimeEqual(a: string, b: string): boolean {
  const ba = Buffer.from(a, "utf8");
  const bb = Buffer.from(b, "utf8");
  if (ba.length !== bb.length) return false;
  return timingSafeEqual(ba, bb);
}

// ─── OTP code generation ──────────────────────────────────────────────────────

export function generateOtp(length = 6): string {
  // Cryptographically secure numeric OTP
  const max = 10 ** length;
  const n = randomBytes(4).readUInt32BE(0) % max;
  return n.toString().padStart(length, "0");
}

// ─── Input validation schemas ─────────────────────────────────────────────────

// Email: RFC-ish, max 254 chars
export const emailSchema = z
  .string()
  .trim()
  .toLowerCase()
  .email("Invalid email")
  .max(254);

// Phone: E.164-ish — + and 7-15 digits
export const phoneSchema = z
  .string()
  .trim()
  .regex(/^\+?[1-9]\d{6,14}$/, "Invalid phone (use +CC...)");

// Either email or phone — the identifier the user types on the login screen
export const contactSchema = z
  .string()
  .trim()
  .min(5, "Too short")
  .max(254, "Too long")
  .refine(
    (v) => emailSchema.safeParse(v).success || phoneSchema.safeParse(v).success,
    "Enter a valid email or phone number",
  );

export const otpSchema = z
  .string()
  .trim()
  .regex(/^\d{6}$/, "OTP must be 6 digits");

export const roleSchema = z.enum(["STUDENT", "TEACHER", "ADMIN"]);

// Chapter / Lesson / Question schemas
export const createChapterSchema = z.object({
  subjectId: z.string().min(1),
  title: z.string().trim().min(2).max(200),
  slug: z
    .string()
    .trim()
    .min(2)
    .max(120)
    .regex(/^[a-z0-9-]+$/, "Slug: lowercase, digits, hyphens only"),
  description: z.string().trim().max(2000).optional(),
  order: z.number().int().min(0).max(10000).default(0),
});

export const createLessonSchema = z.object({
  chapterId: z.string().min(1),
  title: z.string().trim().min(2).max(200),
  slug: z
    .string()
    .trim()
    .min(2)
    .max(120)
    .regex(/^[a-z0-9-]+$/),
  type: z.enum(["TEXT", "VIDEO", "PDF", "INTERACTIVE"]).default("TEXT"),
  content: z.string().max(500_000).default(""),
  videoUrl: z.string().url().optional().or(z.literal("")),
  durationMin: z.number().int().min(1).max(600).default(10),
  order: z.number().int().min(0).max(10000).default(0),
});

export const createQuestionSchema = z.object({
  chapterId: z.string().optional(),
  type: z.enum([
    "SINGLE_CHOICE",
    "MULTIPLE_CHOICE",
    "TRUE_FALSE",
    "ONE_WORD",
    "SHORT_ANSWER",
    "LONG_ANSWER",
    "FILL_BLANK",
    "MATCHING",
  ]),
  difficulty: z.enum(["EASY", "MEDIUM", "HARD"]).default("MEDIUM"),
  stem: z.string().trim().min(3).max(5000),
  options: z.array(z.string().min(1).max(500)).max(10).optional(),
  correctAnswer: z.string().max(2000).optional(),
  explanation: z.string().max(5000).optional(),
  tags: z.array(z.string().min(1).max(40)).max(20).optional(),
});

// ─── HTML / Markdown sanitization stub (defense in depth) ─────────────────────
// For full HTML sanitization use DOMPurify on the client and a server-side
// sanitizer like `sanitize-html` for storage. Here we keep storage as markdown
// and render with react-markdown (no raw HTML by default).

export function sanitizeFilename(name: string): string {
  return name.replace(/[^a-zA-Z0-9._-]/g, "_").slice(0, 200);
}

// ─── Rate limit key builder ───────────────────────────────────────────────────

export function rateLimitKey(action: string, identifier: string): string {
  return `rl:${action}:${sha256(identifier).slice(0, 24)}`;
}
