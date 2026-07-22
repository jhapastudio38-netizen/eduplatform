import { NextResponse, NextRequest } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";

/**
 * GET /api/student/tests/[testId]
 * Returns the test with questions & options (but NEVER the correct answers).
 */
export async function GET(_req: NextRequest, ctx: { params: Promise<{ testId: string }> }) {
  const { testId } = await ctx.params;
  const user = await getCurrentUser();
  if (!user) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });

  const test = await db.test.findUnique({
    where: { id: testId, isPublished: true },
    include: {
      items: {
        orderBy: { order: "asc" },
        include: {
          question: {
            select: {
              id: true, type: true, difficulty: true, stem: true,
              options: true,
              imageUrl: true, audioUrl: true,
            },
          },
        },
      },
    },
  });
  if (!test) return NextResponse.json({ error: "Not found" }, { status: 404 });

  // Check if exam is active
  if (!test.isActive) {
    return NextResponse.json({ error: "This exam has been deactivated." }, { status: 403 });
  }

  // Check if exam window hasn't started yet
  if (test.startAt && new Date(test.startAt) > new Date()) {
    return NextResponse.json({ error: "This exam hasn't started yet." }, { status: 403 });
  }

  // Check if exam window has ended
  if (test.endAt && new Date(test.endAt) < new Date()) {
    return NextResponse.json({ error: "This exam has ended." }, { status: 403 });
  }

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
        id: i.id, testId: i.testId, questionId: i.questionId,
        points: i.points, order: i.order,
        question: {
          id: i.question.id, type: i.question.type, difficulty: i.question.difficulty,
          stem: i.question.stem,
          options: i.question.options ? JSON.parse(i.question.options) : null,
        },
      })),
    },
    submissionId: draft.id,
  });
}
