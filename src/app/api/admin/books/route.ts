/**
 * GET /api/student/books — list published books
 * POST /api/admin/books — admin creates a book (requires ADMIN role)
 */
import { NextResponse, NextRequest } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";
import { z } from "zod";

export async function GET() {
  const books = await db.book.findMany({
    where: { isPublished: true },
    orderBy: { createdAt: "desc" },
    include: { _count: { select: { chapters: true } } },
  });
  return NextResponse.json({ books });
}

const createBookSchema = z.object({
  title: z.string().trim().min(2).max(200),
  slug: z.string().trim().min(2).max(120).regex(/^[a-z0-9-]+$/),
  description: z.string().max(2000).optional(),
  author: z.string().max(100).optional(),
  coverUrl: z.string().url().optional().or(z.literal("")),
  pdfUrl: z.string().url().optional().or(z.literal("")),
  pageCount: z.number().int().min(1).max(10000).optional(),
  category: z.string().max(50).optional(),
  level: z.string().max(50).optional(),
  isPublished: z.boolean().default(false),
});

export async function POST(req: NextRequest) {
  const user = await getCurrentUser();
  if (!user || user.role !== "ADMIN") {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  const body = await req.json().catch(() => null);
  const parsed = createBookSchema.safeParse(body);
  if (!parsed.success) {
    return NextResponse.json({ error: parsed.error.issues[0]?.message }, { status: 400 });
  }
  try {
    const book = await db.book.create({ data: parsed.data });
    return NextResponse.json({ book });
  } catch {
    return NextResponse.json({ error: "Slug already exists" }, { status: 409 });
  }
}
