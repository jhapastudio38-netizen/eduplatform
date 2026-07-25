import { NextResponse, NextRequest } from "next/server";
import { db } from "@/lib/db";

/**
 * GET /api/student/tests?filter=practice|exam|ubt|free|batch|all&category=exam|demo|batch|chapter|question_bank
 *
 * Returns published tests, optionally filtered.
 * - category: filters by testCategory field (exam, demo, batch, chapter, question_bank)
 * - filter: legacy filter (practice, exam, ubt, free, batch, all)
 */
export async function GET(req: NextRequest) {
  const filter = req.nextUrl.searchParams.get("filter") || "all";
  const category = req.nextUrl.searchParams.get("category");

  // Build the where clause
  const where: any = { isPublished: true };

  // New category filter takes priority
  if (category && ["exam", "demo", "batch", "chapter", "question_bank"].includes(category)) {
    where.testCategory = category;
  } else {
    // Legacy filter
    switch (filter) {
      case "practice":
        where.isExam = false;
        break;
      case "exam":
        where.isExam = true;
        break;
      case "ubt":
        where.examType = "UBT";
        break;
      case "free":
        where.isExam = false;
        where.examType = "REGULAR";
        break;
      case "batch":
        where.testCategory = "batch";
        break;
    }
  }

  const tests = await db.test.findMany({
    where,
    orderBy: { createdAt: "desc" },
    include: {
      _count: { select: { items: true } },
    },
  });

  const out = tests.map((t) => ({
    id: t.id,
    title: t.title,
    description: t.description,
    durationMin: t.durationMin,
    isExam: t.isExam,
    examType: t.examType,
    passScore: t.passScore,
    startAt: t.startAt,
    endAt: t.endAt,
    isPublished: t.isPublished,
    isActive: t.isActive,
    // New fields for app display
    featuredImage: t.featuredImage || null,
    category: t.category || null,
    price: t.price ?? null,
    audioPlayMode: t.audioPlayMode || "single",
    audioGapSec: t.audioGapSec ?? 2,
    textBlockCount: t.textBlockCount ?? 20,
    audioBlockCount: t.audioBlockCount ?? 20,
    items: [], // do not leak questions on the listing
    questionCount: t._count.items,
  }));
  return NextResponse.json({ tests: out });
}
