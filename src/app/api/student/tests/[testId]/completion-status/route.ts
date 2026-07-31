/**
 * GET /api/student/tests/[testId]/completion-status
 *
 * Returns whether the current user has already completed this exam.
 */
import { NextResponse, NextRequest } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";

export async function GET(req: NextRequest, ctx: { params: Promise<{ testId: string }> }) {
  const user = await getCurrentUser(req);
  if (!user) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });

  const { testId } = await ctx.params;

  const fullUser = await db.user.findUnique({
    where: { id: user.id },
    select: { subscribedUntil: true, subscriptionType: true },
  });
  const isSubscribed = fullUser?.subscribedUntil
    ? fullUser.subscribedUntil > new Date()
    : false;

  const submission = await db.submission.findUnique({
    where: { testId_userId: { testId, userId: user.id } },
    select: { submittedAt: true, score: true, maxScore: true },
  });

  const completed = !!(submission && submission.submittedAt);
  const canRetake = isSubscribed || !completed;

  return NextResponse.json({
    completed,
    canRetake,
    isSubscribed,
    submittedAt: submission?.submittedAt?.toISOString() || null,
    score: submission?.score ?? null,
    maxScore: submission?.maxScore ?? null,
  });
}
