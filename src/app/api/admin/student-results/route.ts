import { NextResponse } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";

/**
 * GET /api/admin/student-results
 *
 * Returns:
 *   - submissions: individual exam submissions (with user + test info)
 *   - students: aggregated per-student stats (backward compat)
 *   - stats: overall summary (total, graded, averagePct, bestPct)
 *
 * Visible to ADMIN and TEACHER.
 */
export async function GET() {
  const user = await getCurrentUser();
  if (!user || (user.role !== "ADMIN" && user.role !== "TEACHER")) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }

  // Individual submissions (most recent first)
  const submissions = await db.submission.findMany({
    where: { submittedAt: { not: null } },
    orderBy: { submittedAt: "desc" },
    take: 500,
    include: {
      user: {
        select: {
          id: true,
          name: true,
          email: true,
        },
      },
      test: {
        select: {
          id: true,
          title: true,
          examType: true,
          testCategory: true,
        },
      },
    },
  });

  // Per-student aggregated stats (legacy)
  const students = await db.user.findMany({
    where: { role: "STUDENT" },
    select: {
      id: true, name: true, email: true, role: true,
      _count: { select: { submissions: { where: { submittedAt: { not: null } } } } },
    },
    take: 200,
  });

  const studentsWithStats = await Promise.all(
    students.map(async (s) => {
      const stats = await db.userStat.findUnique({ where: { userId: s.id } });
      return { ...s, stats };
    }),
  );

  // Compute summary
  const graded = submissions.filter((s) => s.score != null && s.maxScore != null && s.maxScore > 0);
  const pcts = graded.map((s) => (s.score! / s.maxScore!) * 100);
  const averagePct = pcts.length > 0 ? Math.round(pcts.reduce((a, b) => a + b, 0) / pcts.length) : 0;
  const bestPct = pcts.length > 0 ? Math.round(Math.max(...pcts)) : 0;

  return NextResponse.json({
    submissions,
    students: studentsWithStats,
    stats: {
      total: submissions.length,
      graded: graded.length,
      averagePct,
      bestPct,
    },
  });
}
