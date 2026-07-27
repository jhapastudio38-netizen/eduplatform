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
 *
 * Special testId formats supported:
 *   • "qbank-combined"     — combined QBank exam (all published question_bank tests)
 *   • "bundle-{bundleId}"  — combined exam from a specific bundle (qbank/batch)
 *
 * For combined exams we look up each question by its real ID (passed as the
 * key in the answers map) directly from the Question table — there's no
 * Test/Question join.
 */
export async function POST(req: NextRequest, ctx: { params: Promise<{ testId: string }> }) {
  const { testId } = await ctx.params;
  const user = await getCurrentUser(req);
  if (!user) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });

  const body = await req.json().catch(() => ({})) as { answers?: Record<string, unknown> };
  const answers = body.answers || {};

  // ─── Branch: combined exam submission (qbank-combined OR bundle-{id}) ──────
  const isQBankCombined = testId === "qbank-combined";
  const isBundleCombined = testId.startsWith("bundle-");
  if (isQBankCombined || isBundleCombined) {
    return handleCombinedSubmit(req, user, testId, answers, isQBankCombined);
  }

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

  const submission = await db.submission.upsert({
    where: { testId_userId: { testId, userId: user.id } },
    create: {
      testId,
      userId: user.id,
      answers: JSON.stringify(answers),
      score,
      maxScore,
      startedAt: new Date(),
      submittedAt: new Date(),
      graded: !needsManualGrading,
    },
    update: {
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

  // ─── Eye Vision auto-trigger ──────────────────────────────────────────────
  let eyeVisionRecommendation: { show: boolean; count: number; reason: string } = {
    show: false,
    count: 0,
    reason: "",
  };
  if (maxScore > 0) {
    const mistakesPct = 100 - pct;
    if (mistakesPct > 30) {
      const count = mistakesPct >= 70 ? 5 : mistakesPct >= 50 ? 4 : 3;
      eyeVisionRecommendation = {
        show: true,
        count,
        reason: `You made ${Math.round(mistakesPct)}% mistakes. Let's check your eye vision with ${count} quick tests.`,
      };
    } else if (mistakesPct >= 15) {
      eyeVisionRecommendation = {
        show: true,
        count: 2,
        reason: `You made ${Math.round(mistakesPct)}% mistakes. A quick eye vision check is recommended.`,
      };
    }
  }

  return NextResponse.json({
    score, maxScore,
    graded: submission.graded,
    submissionId: submission.id,
    completed: true, // mark as completed for the app
    review,
    eyeVision: eyeVisionRecommendation,
  });
}

/**
 * Handle combined exam submissions (qbank-combined OR bundle-{id}).
 *
 * We don't have a real Test row for these — we look up each question directly
 * by its ID. The submission is stored with the synthetic testId so we can
 * query "has the user completed this combined exam?" later.
 */
async function handleCombinedSubmit(
  req: NextRequest,
  user: { id: string },
  testId: string,
  answers: Record<string, unknown>,
  isQBankCombined: boolean,
) {
  // Collect all question IDs from the answers
  const questionIds = Object.keys(answers);
  if (questionIds.length === 0) {
    return NextResponse.json({ error: "No answers submitted" }, { status: 400 });
  }

  // Fetch all the questions in one shot
  const questions = await db.question.findMany({
    where: { id: { in: questionIds } },
  });

  let score = 0;
  let maxScore = 0;
  let needsManualGrading = false;

  const review: any[] = [];

  for (const q of questions) {
    // Each question is worth 1 point in combined exams
    const points = 1;
    maxScore += points;
    const ans = answers[q.id];
    const correct = q.correctAnswer ? JSON.parse(q.correctAnswer) : null;
    let isCorrect = false;

    if (q.type === "SINGLE_CHOICE" || q.type === "TRUE_FALSE" || q.type === "ONE_WORD" || q.type === "FILL_BLANK") {
      if (typeof ans === "string" && correct && String(ans).trim().toLowerCase() === String(correct).trim().toLowerCase()) {
        isCorrect = true;
        score += points;
      }
    } else if (q.type === "MULTIPLE_CHOICE") {
      const selected = Array.isArray(ans) ? (ans as string[]).slice().sort() : [];
      const correctArr = Array.isArray(correct) ? (correct as string[]).slice().sort() : [];
      if (selected.length === correctArr.length && selected.every((v, i) => v === correctArr[i])) {
        isCorrect = true;
        score += points;
      }
    } else {
      needsManualGrading = true;
    }

    review.push({
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
    });
  }

  // Save the submission. The unique constraint (testId, userId) means each
  // user can submit each combined exam once — re-submitting overwrites.
  const existingSubmission = await db.submission.findUnique({
    where: { testId_userId: { testId, userId: user.id } },
  });

  const submissionData = {
    answers: JSON.stringify(answers),
    score,
    maxScore,
    startedAt: existingSubmission?.startedAt ?? new Date(),
    submittedAt: new Date(),
    graded: !needsManualGrading,
  };

  let submission;
  if (existingSubmission) {
    submission = await db.submission.update({
      where: { id: existingSubmission.id },
      data: submissionData,
    });
  } else {
    // We need to provide a valid testId FK for Submission — but for combined
    // exams there is no real Test row. To keep the FK happy, we create a
    // virtual Test row on first submission (only once per combined testId).
    // Use upsert to avoid race conditions.
    await db.test.upsert({
      where: { id: testId },
      create: {
        id: testId,
        title: isQBankCombined ? "Question Bank — All Questions" : "Combined Bundle Exam",
        testCategory: isQBankCombined ? "question_bank" : "bundle",
        examType: "REGULAR",
        isExam: false,
        isPublished: true,
        isActive: true,
        durationMin: 60,
        createdBy: user.id,
      },
      update: {},
    });

    submission = await db.submission.create({
      data: {
        testId,
        userId: user.id,
        ...submissionData,
      },
    });
  }

  // Update user stats
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

  const stats = await db.userStat.findUnique({ where: { userId: user.id } });
  if (stats && stats.totalExamsTaken > 0) {
    const newAvg = stats.averageScore + (pct - stats.averageScore) / stats.totalExamsTaken;
    await db.userStat.update({
      where: { userId: user.id },
      data: { averageScore: Math.round(newAvg * 100) / 100 },
    });
  }

  await audit({
    actorId: user.id,
    action: "submit_combined_test",
    entity: "Submission",
    entityId: submission.id,
    metadata: { testId, score, maxScore, questionCount: questions.length },
    ip: req.headers.get("x-forwarded-for")?.split(",")[0],
  });

  // Eye Vision auto-trigger
  let eyeVisionRecommendation: { show: boolean; count: number; reason: string } = {
    show: false,
    count: 0,
    reason: "",
  };
  if (maxScore > 0) {
    const mistakesPct = 100 - pct;
    if (mistakesPct > 30) {
      const count = mistakesPct >= 70 ? 5 : mistakesPct >= 50 ? 4 : 3;
      eyeVisionRecommendation = {
        show: true,
        count,
        reason: `You made ${Math.round(mistakesPct)}% mistakes. Let's check your eye vision with ${count} quick tests.`,
      };
    } else if (mistakesPct >= 15) {
      eyeVisionRecommendation = {
        show: true,
        count: 2,
        reason: `You made ${Math.round(mistakesPct)}% mistakes. A quick eye vision check is recommended.`,
      };
    }
  }

  return NextResponse.json({
    score,
    maxScore,
    graded: submission.graded,
    submissionId: submission.id,
    completed: true, // mark as completed
    review,
    eyeVision: eyeVisionRecommendation,
  });
}
