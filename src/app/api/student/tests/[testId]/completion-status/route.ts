import { NextResponse, NextRequest } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";

/**
 * GET /api/student/tests/[testId]/completion-status
 * Returns whether the student has already completed this test.
 */
export async function GET(req: NextRequest, ctx: { params: Promise<{ testId: string }> }) {
  const { testId } = await ctx.params;
  const user = await getCurrentUser(req);
  if (!user) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });

  // Check if there's a submitted (non-draft) submission for this test+user
  const submission = await db.submission.findFirst({
    where: {
      testId,
      userId: user.id,
      submittedAt: { not: null },
    },
    orderBy: { submittedAt: "desc" },
  });

  // Check subscription status
  const isSubscribed = user.role === "ADMIN" || user.role === "TEACHER" ||
    !!(await db.subscription.findFirst({
      where: {
        userId: user.id,
        expiresAt: { gt: new Date() },
      },
    }));

  return NextResponse.json({
    completed: !!submission,
    isSubscribed,
    score: submission?.score ?? null,
    maxScore: submission?.maxScore ?? null,
  });
}
