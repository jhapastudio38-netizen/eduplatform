/**
 * GET /api/admin/books — list all books (admin only)
 * POST /api/admin/books — admin creates a book (requires ADMIN role)
 */
import { NextResponse, NextRequest } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";
import { z } from "zod";

export async function GET() {
  const user = await getCurrentUser();
  if (!user || (user.role !== "ADMIN" && user.role !== "TEACHER")) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  const books = await db.book.findMany({
    orderBy: { createdAt: "desc" },
    include: { _count: { select: { chapters: true } } },
  });
  return NextResponse.json({ books });
}

const createBookSchema = z.object({
  title: z.string().trim().min(2).max(200),
  // slug is optional — auto-generated from title if not provided
  slug: z.string().trim().min(2).max(120).regex(/^[a-z0-9-]+$/).optional(),
  description: z.string().max(2000).optional().or(z.literal("")),
  author: z.string().max(100).optional().or(z.literal("")),
  // Accept relative paths (/api/files/...) OR absolute URLs (https://...)
  coverUrl: z
    .string()
    .max(2000)
    .optional()
    .or(z.literal(""))
    .refine(
      (v) => !v || v.startsWith("/") || /^https?:\/\//.test(v),
      "Must be a relative path or absolute URL",
    ),
  pdfUrl: z
    .string()
    .max(2000)
    .optional()
    .or(z.literal(""))
    .refine(
      (v) => !v || v.startsWith("/") || /^https?:\/\//.test(v),
      "Must be a relative path or absolute URL",
    ),
  pageCount: z.number().int().min(1).max(10000).optional(),
  category: z.string().max(50).optional().or(z.literal("")),
  level: z.string().max(50).optional().or(z.literal("")),
  publishedDate: z.string().max(50).optional().or(z.literal("")),
  isPublished: z.boolean().default(false),
});

// Auto-generate slug from title: lowercase, spaces → hyphens, strip non-alphanumeric
function slugify(input: string): string {
  return input
    .toLowerCase()
    .trim()
    .replace(/[^a-z0-9\s-]/g, "")
    .replace(/\s+/g, "-")
    .replace(/-+/g, "-")
    .slice(0, 80);
}

export async function POST(req: NextRequest) {
  const user = await getCurrentUser();
  if (!user || (user.role !== "ADMIN" && user.role !== "TEACHER")) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  const body = await req.json().catch(() => null);
  const parsed = createBookSchema.safeParse(body);
  if (!parsed.success) {
    return NextResponse.json({ error: parsed.error.issues[0]?.message }, { status: 400 });
  }
  const d = parsed.data;
  // Auto-generate slug from title if not provided — ensure uniqueness
  let slug = d.slug || slugify(d.title);
  let n = 1;
  while (await db.book.findUnique({ where: { slug } })) {
    slug = `${slugify(d.title)}-${n++}`;
  }
  try {
    const book = await db.book.create({
      data: {
        title: d.title,
        slug,
        description: d.description || null,
        author: d.author || null,
        coverUrl: d.coverUrl || null,
        pdfUrl: d.pdfUrl || null,
        pageCount: d.pageCount ?? null,
        category: d.category || null,
        level: d.level || null,
        publishedDate: d.publishedDate || null,
        isPublished: d.isPublished,
      },
    });
    return NextResponse.json({ book });
  } catch (e: any) {
    return NextResponse.json({ error: e.message?.substring(0, 200) || "Could not create book" }, { status: 500 });
  }
}
