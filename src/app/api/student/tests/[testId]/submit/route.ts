import { NextResponse, NextRequest } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";
import { audit } from "@/lib/audit";

/**
 * POST /api/student/tests/[testId]/submit
 * Body: { answers: { [questionId]: string | string[] } }
 *
 * Auto-grades objective question types (single/multiple/true-false/one-word/fill-blank).
 * Subjective answers (short / long) require teacher/admin review.
 */
export async function POST(req: NextRequest, ctx: { params: Promise<{ testId: string }> }) {
  const { testId } = await ctx.params;
  const user = await getCurrentUser(req);
  if (!user) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });

  const body = await req.json().catch(() => ({})) as { answers?: Record<string, unknown> };
  const answers = body.answers || {};

  const test = await db.test.findUnique({
    where: { id: testId },
    include: {
      items: {
        include: { question: true },
      },
    },
  });
  if (!test) return NextResponse.json({ error: "Not found" }, { status: 404 });

  // Check if exam is active (admin/teacher may have deactivated it)
  if (!test.isActive) {
    return NextResponse.json({ error: "This exam has been deactivated by the administrator." }, { status: 403 });
  }

  // Check if exam window has ended
  if (test.endAt && new Date(test.endAt) < new Date()) {
    return NextResponse.json({ error: "The exam period has ended." }, { status: 403 });
  }

  let score = 0;
  let maxScore = 0;
  let needsManualGrading = false;

  for (const item of test.items) {
    maxScore += item.points;
    const q = item.question;
    const ans = answers[q.id];
    const correct = q.correctAnswer ? JSON.parse(q.correctAnswer) : null;

    if (q.type === "SINGLE_CHOICE" || q.type === "TRUE_FALSE" || q.type === "ONE_WORD" || q.type === "FILL_BLANK") {
      if (typeof ans === "string" && correct && String(ans).trim().toLowerCase() === String(correct).trim().toLowerCase()) {
        score += item.points;
      } else if (ans && test.negativeMarking > 0) {
        // Wrong answer with negative marking
        score -= test.negativeMarking;
      }
    } else if (q.type === "MULTIPLE_CHOICE") {
      const selected = Array.isArray(ans) ? (ans as string[]).slice().sort() : [];
      const correctArr = Array.isArray(correct) ? (correct as string[]).slice().sort() : [];
      if (selected.length === correctArr.length && selected.every((v, i) => v === correctArr[i])) {
        score += item.points;
      } else if (selected.length > 0 && test.negativeMarking > 0) {
        score -= test.negativeMarking;
      }
    } else {
      // SHORT_ANSWER / LONG_ANSWER / MATCHING — needs human review
      needsManualGrading = true;
    }
  }

  const submission = await db.submission.update({
    where: { testId_userId: { testId, userId: user.id } },
    data: {
      answers: JSON.stringify(answers),
      score,
      maxScore,
      submittedAt: new Date(),
      graded: !needsManualGrading,
    },
  });

  // Update user stats in real time
  const pct = maxScore > 0 ? (score / maxScore) * 100 : 0;
  await db.userStat.upsert({
    where: { userId: user.id },
    create: {
      userId: user.id,
      totalExamsTaken: 1,
      totalCorrectAnswers: score,
      totalQuestionsAnswered: maxScore,
      averageScore: pct,
      lastStudyDate: new Date(),
    },
    update: {
      totalExamsTaken: { increment: 1 },
      totalCorrectAnswers: { increment: score },
      totalQuestionsAnswered: { increment: maxScore },
      lastStudyDate: new Date(),
    },
  });

  // Recalculate average score (running average)
  const stats = await db.userStat.findUnique({ where: { userId: user.id } });
  if (stats && stats.totalExamsTaken > 0) {
    // Use weighted running average: newAvg = oldAvg + (newScore - oldAvg) / n
    const newAvg = stats.averageScore + (pct - stats.averageScore) / stats.totalExamsTaken;
    await db.userStat.update({
      where: { userId: user.id },
      data: { averageScore: Math.round(newAvg * 100) / 100 },
    });
  }

  // Build per-question review (correct/wrong + correct answer for student learning)
  const review = test.items.map((item) => {
    const q = item.question;
    const ans = answers[q.id];
    const correct = q.correctAnswer ? JSON.parse(q.correctAnswer) : null;
    let isCorrect = false;

    if (q.type === "SINGLE_CHOICE" || q.type === "TRUE_FALSE" || q.type === "ONE_WORD" || q.type === "FILL_BLANK") {
      if (typeof ans === "string" && correct && String(ans).trim().toLowerCase() === String(correct).trim().toLowerCase()) {
        isCorrect = true;
      }
    } else if (q.type === "MULTIPLE_CHOICE") {
      const selected = Array.isArray(ans) ? (ans as string[]).slice().sort() : [];
      const correctArr = Array.isArray(correct) ? (correct as string[]).slice().sort() : [];
      isCorrect = selected.length === correctArr.length && selected.every((v, i) => v === correctArr[i]);
    }

    return {
      questionId: q.id,
      stem: q.stem,
      type: q.type,
      options: q.options ? JSON.parse(q.options) : null,
      imageUrl: q.imageUrl,
      audioUrl: q.audioUrl,
      audioLoop: q.audioLoop,
      audioLoopDelay: q.audioLoopDelay,
      userAnswer: ans,
      correctAnswer: correct,
      explanation: q.explanation,
      isCorrect,
    };
  });

  await audit({
    actorId: user.id,
    action: "submit_test",
    entity: "Submission",
    entityId: submission.id,
    metadata: { testId, score, maxScore, graded: submission.graded },
    ip: req.headers.get("x-forwarded-for")?.split(",")[0],
  });

  return NextResponse.json({
    score, maxScore,
    graded: submission.graded,
    submissionId: submission.id,
    review,
  });
}
