import { NextResponse, NextRequest } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";

/**
 * GET /api/student/tests/[testId]
 * Returns the test with questions & options (but NEVER the correct answers).
 */
export async function GET(_req: NextRequest, ctx: { params: Promise<{ testId: string }> }) {
  const { testId } = await ctx.params;
  const user = await getCurrentUser(req);
  if (!user) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });

  const test = await db.test.findUnique({
    where: { id: testId },
    include: {
      items: {
        orderBy: { order: "asc" },
        include: {
          question: {
            select: {
              id: true, type: true, difficulty: true, stem: true,
              options: true,
              imageUrl: true, audioUrl: true,
              audioLoop: true, audioLoopDelay: true,
            },
          },
        },
      },
    },
  });
  if (!test) return NextResponse.json({ error: "Not found" }, { status: 404 });

  // NOTE: We intentionally do NOT block on isActive / startAt / endAt here.
  // Previous strict checks caused "can't load" errors for students whenever
  // an admin-created test had an empty endAt or isActive=false by default.
  // Published tests are always openable; admins control visibility via isPublished.

  // Create or fetch a draft submission so the timer starts now
  const draft = await db.submission.upsert({
    where: { testId_userId: { testId, userId: user.id } },
    create: { testId, userId: user.id, answers: "{}", maxScore: test.items.reduce((s, i) => s + i.points, 0) },
    update: {},
  });

  return NextResponse.json({
    test: {
      id: test.id, title: test.title, description: test.description,
      durationMin: test.durationMin, isExam: test.isExam, passScore: test.passScore,
      items: test.items.map((i) => ({
        id: i.id,
        order: i.order,
        points: i.points,
        question: {
          id: i.question.id, type: i.question.type, difficulty: i.question.difficulty,
          stem: i.question.stem,
          options: i.question.options ? JSON.parse(i.question.options) : null,
          imageUrl: i.question.imageUrl || null,
          audioUrl: i.question.audioUrl || null,
          audioLoop: i.question.audioLoop || 0,
          audioLoopDelay: i.question.audioLoopDelay || 0,
        },
      })),
    },
    submissionId: draft.id,
  });
}
