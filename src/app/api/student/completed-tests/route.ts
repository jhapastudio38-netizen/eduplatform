/**
 * GET /api/student/completed-tests
 *
 * Returns the list of test IDs (including combined exam IDs like
 * "qbank-combined" or "bundle-{id}") that the current user has completed
 * (i.e. submitted at least once).
 *
 * Used by the Android app to show a "Completed" badge on exam cards and to
 * prevent accidental re-attempts when the admin has set maxAttempts=1.
 */
import { NextResponse, NextRequest } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";

export async function GET(req: NextRequest) {
  const user = await getCurrentUser(req);
  if (!user) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });

  const submissions = await db.submission.findMany({
    where: {
      userId: user.id,
      submittedAt: { not: null },
    },
    select: {
      testId: true,
      submittedAt: true,
      score: true,
      maxScore: true,
    },
    orderBy: { submittedAt: "desc" },
  });

  // Return as a map: testId -> { submittedAt, score, maxScore }
  const completed: Record<string, { submittedAt: string; score: number | null; maxScore: number | null }> = {};
  for (const s of submissions) {
    completed[s.testId] = {
      submittedAt: s.submittedAt!.toISOString(),
      score: s.score,
      maxScore: s.maxScore,
    };
  }

  return NextResponse.json({ completed, total: submissions.length });
}
