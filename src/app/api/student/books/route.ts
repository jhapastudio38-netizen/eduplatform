/**
 * GET /api/student/books — student view of published books
 */
import { NextResponse } from "next/server";
import { db } from "@/lib/db";

export async function GET() {
  const books = await db.book.findMany({
    where: { isPublished: true },
    orderBy: { createdAt: "desc" },
    select: {
      id: true, title: true, slug: true, description: true,
      author: true, coverUrl: true, pdfUrl: true, pageCount: true,
      category: true, level: true, downloads: true,
    },
  });
  return NextResponse.json({ books });
}
