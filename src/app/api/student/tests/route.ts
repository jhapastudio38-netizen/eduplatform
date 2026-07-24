import { NextResponse, NextRequest } from "next/server";
import { db } from "@/lib/db";

/**
 * GET /api/student/tests?filter=practice|exam|ubt|free|batch|all
 *
 * Returns published tests, optionally filtered.
 *  - practice : isExam = false (free practice tests)
 *  - exam     : isExam = true (formal exams)
 *  - ubt      : examType = "UBT"
 *  - free     : examType = "REGULAR" AND isExam = false
 *  - batch    : examType = "BATCH"
 *  - all      : no filter (default)
 */
export async function GET(req: NextRequest) {
  const filter = req.nextUrl.searchParams.get("filter") || "all";

  // Build the where clause based on the filter
  const where: any = { isPublished: true };
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
      where.examType = "BATCH";
      break;
    // "all" or anything else → no extra filter
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
    items: [], // do not leak questions on the listing
    questionCount: t._count.items,
  }));
  return NextResponse.json({ tests: out });
}
