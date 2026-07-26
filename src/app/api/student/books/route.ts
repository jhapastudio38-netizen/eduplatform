/**
 * GET /api/student/books — student view of published books
 */
import { NextResponse, NextRequest } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";

export async function GET(req: NextRequest) {
  const user = await getCurrentUser(req);
  if (!user) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });

  const books = await db.book.findMany({
    where: { isPublished: true },
    orderBy: { createdAt: "desc" },
    select: {
      id: true, title: true, slug: true, description: true,
      author: true, coverUrl: true, pdfUrl: true, pageCount: true,
      category: true, level: true, publishedDate: true, downloads: true,
    },
  });

  const BASE = "https://my-project-five-sepia.vercel.app";
  const out = books.map(b => ({
    ...b,
    coverUrl: b.coverUrl ? (b.coverUrl.startsWith("http") ? b.coverUrl : BASE + b.coverUrl) : null,
    pdfUrl: b.pdfUrl ? (b.pdfUrl.startsWith("http") ? b.pdfUrl : BASE + b.pdfUrl) : null,
  }));

  return NextResponse.json({ books: out });
}
